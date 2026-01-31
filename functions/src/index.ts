import * as functionsV1 from "firebase-functions/v1";
import * as admin from "firebase-admin";
import { Request, Response } from "express";
import * as crypto from "crypto";
import { onCall, HttpsError } from "firebase-functions/v2/https";
import * as nodemailer from "nodemailer";

// iyzico için tip tanımları
admin.initializeApp();
const db = admin.firestore();

// Nodemailer Transporter
const transporter = nodemailer.createTransport({
    service: "gmail",
    auth: {
        user: process.env.EMAIL_USER,
        pass: process.env.EMAIL_PASS
    }
});

// ==================== BİLDİRİM YARDIMCISI ====================

async function sendNotification(userId: string, title: string, body: string, data: any = {}) {
    const userDoc = await db.collection("users").doc(userId).get();
    const fcmToken = userDoc.data()?.fcmToken;

    if (!fcmToken) {
        console.log(`No FCM token for user ${userId}`);
        return;
    }

    const message = {
        token: fcmToken,
        notification: { title, body },
        data: { ...data, click_action: "FLUTTER_NOTIFICATION_CLICK" },
        android: {
            priority: "high" as const,
            notification: {
                sound: "default",
                channelId: "high_priority_notifications"
            }
        }
    };

    try {
        await admin.messaging().send(message);
        console.log(`Notification sent to ${userId}`);
    } catch (error) {
        console.error("Error sending notification:", error);
    }
}

// ==================== GÖREV TETİKLEYİCİLERİ (ALGORİTMA) ====================

export const onTaskStatusChanged = functionsV1.firestore
    .document("tasks/{taskId}")
    .onUpdate(async (change: any, context: any) => {
        const before = change.before.data();
        const after = change.after.data();
        const taskId = context.params.taskId;

        // 1. Görev Atandı
        if (before.status === "OPEN" && after.status === "ASSIGNED" && after.workerId) {
            await sendNotification(
                after.workerId,
                "🎉 Görev Size Atandı!",
                `"${after.title}" görevi için seçildiniz. Hemen başlayabilirsiniz.`,
                { taskId, type: "TASK_ASSIGNED" }
            );
        }

        // 2. İş Teslim Edildi
        if (before.status === "ASSIGNED" && after.status === "DELIVERED") {
            await sendNotification(
                after.creatorId,
                "📦 İş Teslim Edildi",
                `"${after.title}" görevi tamamlandı olarak işaretlendi. Lütfen onaylayın.`,
                { taskId, type: "TASK_DELIVERED" }
            );
        }

        // 3. Ödeme Kilidi Açma (ALGORİTMA: Hem İşçi Hem İşveren Onayladığında)
        if (after.creatorConfirmed && after.workerConfirmed && after.status !== "COMPLETED" && after.paymentStatus !== "RELEASED") {
            console.log(`[Algorithm] Releasing payment for task ${taskId}`);
            const amount = after.rewardAmount;
            const workerId = after.workerId;
            const creatorId = after.creatorId;

            await db.runTransaction(async (transaction: any) => {
                const workerRef = db.collection("users").doc(workerId);
                const creatorRef = db.collection("users").doc(creatorId);
                const taskRef = db.collection("tasks").doc(taskId);

                transaction.update(workerRef, {
                    balance: admin.firestore.FieldValue.increment(amount),
                    completedTasks: admin.firestore.FieldValue.increment(1)
                });
                transaction.update(creatorRef, {
                    escrowBalance: admin.firestore.FieldValue.increment(-amount)
                });
                transaction.update(taskRef, {
                    status: "COMPLETED",
                    paymentStatus: "RELEASED",
                    completedAt: admin.firestore.FieldValue.serverTimestamp()
                });

                // Transaction kaydı
                const txRef = db.collection("transactions").doc();
                transaction.set(txRef, {
                    userId: workerId,
                    type: "EARNING",
                    amount,
                    description: `Görev kazancı: ${after.title}`,
                    taskId,
                    status: "completed",
                    createdAt: admin.firestore.FieldValue.serverTimestamp()
                });
            });

            await sendNotification(workerId, "💰 Ödeme Cüzdanınızda!", `${amount} TL kazancınız hesabınıza eklendi.`);
        }
    });

/**
 * Görev Oluşturma (ALGORİTMA: Provizyonlu ve Güvenli)
 */
export const createTaskV2 = onCall({ cors: true }, async (request) => {
    if (!request.auth) throw new HttpsError("unauthenticated", "Giriş yapmalısınız.");

    const taskData = request.data;
    const userId = request.auth.uid;
    const amount = taskData.rewardAmount;

    if (!amount || amount < 10) throw new HttpsError("invalid-argument", "Geçersiz ödül tutarı.");

    return await db.runTransaction(async (transaction: any) => {
        const userRef = db.collection("users").doc(userId);
        const userDoc = await transaction.get(userRef);

        if (!userDoc.exists) throw new Error("Kullanıcı bulunamadı.");
        const currentBalance = userDoc.data()?.balance || 0;

        if (currentBalance < amount) {
            throw new Error("Yetersiz bakiye! Lütfen cüzdanınıza bakiye yükleyin.");
        }

        // 1. Bakiyeyi Düş ve Escrow'a Al
        transaction.update(userRef, {
            balance: admin.firestore.FieldValue.increment(-amount),
            escrowBalance: admin.firestore.FieldValue.increment(amount),
            publishedTasks: admin.firestore.FieldValue.increment(1)
        });

        // 2. Görev Dokümanını Oluştur
        const taskRef = db.collection("tasks").doc();
        transaction.set(taskRef, {
            ...taskData,
            creatorId: userId,
            workerId: null,
            status: "OPEN",
            paymentStatus: "PENDING",
            creatorConfirmed: false,
            workerConfirmed: false,
            createdAt: admin.firestore.FieldValue.serverTimestamp(),
            requestedBy: []
        });

        // 3. İşlem Kaydı
        const txRef = db.collection("transactions").doc();
        transaction.set(txRef, {
            userId,
            amount: -amount,
            type: "TASK_PAYMENT",
            description: "Görev ücreti bloke edildi (Escrow)",
            taskId: taskRef.id,
            status: "completed",
            createdAt: admin.firestore.FieldValue.serverTimestamp()
        });

        return { success: true, taskId: taskRef.id };
    }).catch(err => {
        throw new HttpsError("internal", err.message);
    });
});

// ==================== CHAT TETİKLEYİCİSİ ====================

export const onNewMessage = functionsV1.firestore
    .document("chats/{chatId}/messages/{messageId}")
    .onCreate(async (snapshot: any, context: any) => {
        const message = snapshot.data();
        const chatId = context.params.chatId;

        // 1. Risk Analizi (AI Moderator Lite)
        const riskyKeywords = ["iban", "banka", "hesap", "053", "054", "055", "wp", "whatsapp", "telegram", "dışarıdan", "elden"];
        const messageText = message.text.toLowerCase();
        const isRisky = riskyKeywords.some(keyword => messageText.includes(keyword));

        if (isRisky) {
            console.warn(`[AI Moderator] Risky content detected in chat ${chatId}: ${message.text}`);
            await db.collection("security_alerts").add({
                type: "RISKY_CHAT",
                chatId,
                senderId: message.senderId,
                content: message.text,
                createdAt: admin.firestore.FieldValue.serverTimestamp(),
                status: "PENDING"
            });

            // Sohbeti işaretle
            await db.collection("chats").doc(chatId).update({
                hasRisk: true,
                lastRiskAt: admin.firestore.FieldValue.serverTimestamp()
            });
        }

        const chatDoc = await db.collection("chats").doc(chatId).get();
        const participants = chatDoc.data()?.participants as string[];

        const receiverId = participants.find(id => id !== message.senderId);
        if (receiverId) {
            await sendNotification(
                receiverId,
                "💬 Yeni Mesaj",
                message.text.length > 50 ? message.text.substring(0, 47) + "..." : message.text,
                { chatId, type: "NEW_MESSAGE" }
            );
        }
    });

// İş Bankası (Nestpay/EST) Konfigürasyonu
const isBankConfig = {
    clientId: process.env.ISBANK_CLIENT_ID || "",
    storeKey: process.env.ISBANK_STORE_KEY || "",
    terminalId: process.env.ISBANK_TERMINAL_ID || "",
    gatewayUrl: "https://sanalpos.isbank.com.tr/fim/est3Dgate",
    okUrl: "https://us-central1-gane-35146.cloudfunctions.net/paymentCallback",
    failUrl: "https://us-central1-gane-35146.cloudfunctions.net/paymentCallback",
};

/**
 * İş Bankası için Güvenlik Hash'i oluştur (SHA-512)
 */
function generateHash(clientId: string, oid: string, amount: string, okUrl: string, failUrl: string, trantype: string, rnd: string, storeKey: string): string {
    // Nestpay hash formatı: clientid + oid + amount + okUrl + failUrl + trantype + rnd + storekey
    const hashStr = clientId + oid + amount + okUrl + failUrl + trantype + rnd + storeKey;
    return crypto.createHash("sha512").update(hashStr).digest("base64");
}

// ==================== ÖDEME BAŞLATMA (İŞ BANKASI) ====================

/**
 * İş Bankası Sanal POS (3D Pay) Ödeme Başlatma
 * Bankanın 3D Secure sayfasına yönlendirme formu oluşturur.
 */
export const initiatePaymentV2 = onCall({ cors: true }, async (request) => {
    if (!request.auth) {
        throw new HttpsError("unauthenticated", "Giriş yapmalısınız.");
    }

    const userId = request.auth.uid;
    const amount = parseFloat(request.data.amount);
    if (isNaN(amount) || amount < 10) {
        throw new HttpsError("invalid-argument", "Minimum 10 TL yüklenebilir.");
    }

    // %8 Komisyon (Hizmet Bedeli) Hesaplama
    const commissionRate = 0.08;
    const serviceFee = amount * commissionRate;
    const totalToPay = amount + serviceFee; // Bankadan çekilecek asıl tutar

    const orderId = `ON${Date.now()}${Math.floor(Math.random() * 1000)}`;
    const amountStr = totalToPay.toFixed(2); // Bankaya giden tutar formatlı
    const rnd = Date.now().toString();

    const hash = generateHash(
        isBankConfig.clientId,
        orderId,
        amountStr,
        isBankConfig.okUrl,
        isBankConfig.failUrl,
        "Auth", // Satış işlemi
        rnd,
        isBankConfig.storeKey
    );

    // Bekleyen ödemeyi kaydet
    await db.collection("pending_payments").doc(orderId).set({
        userId,
        amount: amount, // Kullanıcının cüzdanına girecek asıl tutar
        formData: {
            clientid: isBankConfig.clientId,
            storetype: "3d_pay",
            trantype: "Auth",
            amount: amountStr,
            oid: orderId,
            okUrl: isBankConfig.okUrl,
            failUrl: isBankConfig.failUrl,
            rnd: rnd,
            hash: hash,
            currency: "949",
            lang: "tr",
            encoding: "UTF-8"
        },
        status: "pending",
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    // Mobil uygulama için direkt açılabilir URL döndür
    const gatewayUrl = "https://us-central1-gane-35146.cloudfunctions.net/renderPaymentForm?oid=" + orderId;

    return {
        success: true,
        paymentPageUrl: gatewayUrl
    };
});

/**
 * İş Bankası için auto-submit HTML formu oluşturur
 */
export const renderPaymentForm = functionsV1.https.onRequest(async (req: Request, res: Response) => {
    const oid = req.query.oid as string;
    if (!oid) {
        res.status(400).send("Geçersiz işlem");
        return;
    }

    const pendingDoc = await db.collection("pending_payments").doc(oid).get();
    if (!pendingDoc.exists) {
        res.status(404).send("İşlem bulunamadı");
        return;
    }

    const formData = pendingDoc.data()?.formData;

    const html = `
        <html>
        <head><title>Ödeme Sayfasına Yönlendiriliyorsunuz...</title></head>
        <body onload="document.forms[0].submit()">
            <form action="${isBankConfig.gatewayUrl}" method="post">
                ${Object.entries(formData).map(([key, value]) => `<input type="hidden" name="${key}" value="${value}" />`).join("")}
            </form>
            <p>Lütfen bekleyin, İş Bankası güvenli ödeme sayfasına yönlendiriliyorsunuz...</p>
        </body>
        </html>
    `;

    res.send(html);
});

// ==================== ÖDEME CALLBACK (İŞ BANKASI) ====================

/**
 * İş Bankası'ndan gelen POST callback'i (Form-data)
 */
export const paymentCallback = functionsV1.https.onRequest(async (req: Request, res: Response) => {
    const data = req.body;
    const { Response, AuthCode, oid, mdStatus } = data;

    console.log(`Payment Callback received for OID: ${oid}, Response: ${Response}`);

    // GÜVENLİK: Hash Doğrulaması (Bankadan geldiğine emin olmak için)
    // Gerçek uygulamada burada gelen parametrelerle hash tekrar hesaplanıp doğrulanmalıdır.

    const pendingDoc = await db.collection("pending_payments").doc(oid).get();
    if (!pendingDoc.exists) {
        console.error("Payment not found for OID:", oid);
        res.redirect("acilnakit://payment-failed?error=not_found");
        return;
    }

    const pendingData = pendingDoc.data()!;
    if (pendingData.status !== "pending") {
        res.redirect("acilnakit://payment-success");
        return;
    }

    // mdStatus == '1' (3D Tamam) ise başarılıdır
    if (Response === "Approved" && (mdStatus === "1" || mdStatus === "2" || mdStatus === "3" || mdStatus === "4")) {
        const userId = pendingData.userId;
        const netAmount = pendingData.amount;


        try {
            await db.runTransaction(async (transaction: any) => {
                const userRef = db.collection("users").doc(userId);
                const userDoc = await transaction.get(userRef);

                if (!userDoc.exists) throw new Error("Kullanıcı yok");

                const currentBalance = userDoc.data()?.balance || 0;
                transaction.update(userRef, { balance: currentBalance + netAmount });

                // Kayıt oluştur
                const txRef = db.collection("transactions").doc();
                transaction.set(txRef, {
                    userId,
                    type: "DEPOSIT",
                    amount: netAmount,
                    description: "İş Bankası Cüzdan Yükleme",
                    status: "completed",
                    orderId: oid,
                    authCode: AuthCode,
                    createdAt: admin.firestore.FieldValue.serverTimestamp(),
                });

                transaction.update(pendingDoc.ref, {
                    status: "completed",
                    completedAt: admin.firestore.FieldValue.serverTimestamp(),
                });
            });

            console.log(`Bakiye yüklendi: ${userId}, Tutar: ${netAmount}`);
            res.redirect(`acilnakit://payment-success?amount=${netAmount}`);
        } catch (error) {
            console.error("Transaction failed:", error);
            res.status(500).send("İşlem hatası");
        }
    } else {
        // Hata durumu
        const errorMsg = data.ErrMsg || "Ödeme banka tarafından reddedildi.";
        await pendingDoc.ref.update({
            status: "failed",
            error: errorMsg,
            failedAt: admin.firestore.FieldValue.serverTimestamp(),
        });

        res.redirect(`acilnakit://payment-failed?error=${encodeURIComponent(errorMsg)}`);
    }
});

// ==================== ESCROW SİSTEMİ ====================

/**
 * Görev tamamlandığında ödemeyi işçiye aktar (24 saat sonra)
 * Scheduled function - Her saat çalışır
 */
/**
 * Otomatik Ödeme Onayı (ALGORİTMA: Mağduriyet Önleyici)
 * İşçi işi teslim etti ama işveren 24 saat boyunca onaylamadıysa, sistem otomatik onaylar.
 */
export const processAutoConfirmTasks = functionsV1.pubsub
    .schedule("every 4 hours")
    .onRun(async () => {
        const now = admin.firestore.Timestamp.now();
        const autoConfirmThreshold = admin.firestore.Timestamp.fromMillis(
            now.toMillis() - 24 * 60 * 60 * 1000 // 24 Saat
        );

        // Teslim edilmiş ama 24 saattir onay bekleyen görevleri bul
        const tasksQuery = await db
            .collection("tasks")
            .where("status", "==", "DELIVERED")
            .where("deliveredAt", "<=", autoConfirmThreshold)
            .where("creatorConfirmed", "==", false)
            .get();

        const promises = tasksQuery.docs.map(taskDoc => {
            console.log(`[Algorithm] Auto-confirming stale task ${taskDoc.id}`);
            return taskDoc.ref.update({
                creatorConfirmed: true,
                autoConfirmedAt: admin.firestore.FieldValue.serverTimestamp()
            });
        });

        await Promise.all(promises);
        return null;
    });

// ==================== ŞİKAYET DURUMUNDA İADE ====================

/**
 * Şikayet sonucuna göre ödemeyi paylaştır (İade veya Ödeme)
 */
export const resolveDispute = functionsV1.firestore
    .document("disputes/{disputeId}")
    .onUpdate(async (change: any, context: any) => {
        const afterData = change.after.data();
        const beforeData = change.before.data();

        // Sadece PENDING'den çözüme geçenleri işle
        if (beforeData.status !== "PENDING" || !afterData.status.startsWith("RESOLVED_")) {
            return null;
        }

        const taskDoc = await db.collection("tasks").doc(afterData.taskId).get();
        const task = taskDoc.data();
        if (!task) return null;

        const { rewardAmount, creatorId, workerId, title } = task;

        // --- SENARYO 1: Görev Sahibi Haklı (İADE) ---
        if (afterData.status === "RESOLVED_CREATOR_WIN") {
            await db.collection("users").doc(creatorId).update({
                balance: admin.firestore.FieldValue.increment(rewardAmount),
                escrowBalance: admin.firestore.FieldValue.increment(-rewardAmount)
            });
            await taskDoc.ref.update({ paymentStatus: "REFUNDED", status: "CANCELLED" });

            await db.collection("transactions").add({
                userId: creatorId,
                type: "REFUND",
                amount: rewardAmount,
                description: `Şikayet İadesi: ${title}`,
                taskId: afterData.taskId,
                createdAt: admin.firestore.FieldValue.serverTimestamp(),
            });

            await sendNotification(creatorId, "💰 İade Onaylandı", `Şikayetiniz sonuçlandı, ${rewardAmount} TL iade edildi.`);
        }

        // --- SENARYO 2: İşi Yapan Haklı (ÖDEME) ---
        else if (afterData.status === "RESOLVED_WORKER_WIN" && workerId) {
            // Creators'dan escrow düş
            await db.collection("users").doc(creatorId).update({
                escrowBalance: admin.firestore.FieldValue.increment(-rewardAmount)
            });
            // Worker'a bakiye ekle
            await db.collection("users").doc(workerId).update({
                balance: admin.firestore.FieldValue.increment(rewardAmount),
            });
            await taskDoc.ref.update({ paymentStatus: "RELEASED", status: "COMPLETED" });

            await db.collection("transactions").add({
                userId: workerId,
                type: "PAYMENT_RECEIVED",
                amount: rewardAmount,
                description: `Şikayet Kararı Ödemesi: ${title}`,
                taskId: afterData.taskId,
                createdAt: admin.firestore.FieldValue.serverTimestamp(),
            });

            await sendNotification(workerId, "✅ Ödemeniz Onaylandı", `Şikayet sonucunda ${rewardAmount} TL bakiyenize eklendi.`);
        }

        return null;
    });

// ==================== PARA ÇEKME ====================

/**
 * Para çekme talebi oluştur
 */
export const requestWithdrawalV2 = onCall({ cors: true }, async (request) => {
    if (!request.auth) {
        throw new HttpsError(
            "unauthenticated",
            "Giriş yapmalısınız."
        );
    }

    const { amount, iban, accountName } = request.data;
    const userId = request.auth.uid;

    if (!amount || amount < 50) {
        throw new HttpsError(
            "invalid-argument",
            "Minimum çekim tutarı 50 TL'dir."
        );
    }

    if (!iban || !accountName) {
        throw new HttpsError(
            "invalid-argument",
            "IBAN ve hesap adı gereklidir."
        );
    }

    // Sabit İşlem Ücreti: 5 TL
    const withdrawalFee = 5.0;
    const totalAmountToDeduct = amount + withdrawalFee;

    // Bakiye kontrolü (Ücret dahil)
    const userDoc = await db.collection("users").doc(userId).get();
    const userData = userDoc.data();

    if (!userData || (userData.balance || 0) < totalAmountToDeduct) {
        throw new HttpsError(
            "failed-precondition",
            `Yetersiz bakiye. İşlem ücreti (5 TL) dahil ${totalAmountToDeduct} TL bakiyeniz olmalıdır.`
        );
    }

    // Bakiyeden düş (Ücret dahil)
    await db
        .collection("users")
        .doc(userId)
        .update({
            balance: admin.firestore.FieldValue.increment(-totalAmountToDeduct),
        });

    // Çekim talebi oluştur
    const withdrawalRef = await db.collection("withdrawals").add({
        userId,
        amount,           // Kullanıcının eline geçecek net tutar
        fee: withdrawalFee,// Alınan komisyon
        totalDeducted: totalAmountToDeduct,
        iban,
        accountName,
        status: "pending",
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    // Transaction kaydı
    await db.collection("transactions").add({
        userId,
        type: "WITHDRAWAL",
        amount: -totalAmountToDeduct,
        description: "Para çekme talebi",
        withdrawalId: withdrawalRef.id,
        status: "pending",
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    return {
        success: true,
        withdrawalId: withdrawalRef.id,
        message: "Para çekme talebiniz alındı. 1-3 iş günü içinde işlenecektir.",
    };
}
);
// ==================== PARA ÇEKME BİLDİRİMİ ====================

/**
 * Para çekme talebi güncellendiğinde kullanıcıya bildirim gönder
 */
export const onWithdrawalUpdated = functionsV1.firestore
    .document("withdrawals/{id}")
    .onUpdate(async (change: any, context: any) => {
        const after = change.after.data();
        const before = change.before.data();

        if (before.status !== "completed" && after.status === "completed") {
            await sendNotification(
                after.userId,
                "💸 Para Çekme Başarılı",
                `${after.amount} TL tutarındaki çekim talebiniz banka hesabınıza gönderilmiştir.`,
                { type: "WITHDRAWAL_COMPLETED" }
            );
        } else if (before.status !== "failed" && after.status === "failed") {
            await sendNotification(
                after.userId,
                "❌ Para Çekme İptal Edildi",
                `Para çekme talebiniz reddedildi: ${after.failureReason || "Banka bilgileri hatalı."}`,
                { type: "WITHDRAWAL_FAILED" }
            );
        }
    });
// ==================== ADMIN AKSİYONLARI ====================

/**
 * Admin Panel'den kullanıcıya mesaj gönder ve bildirim bas
 */
export const sendAdminMessageV2 = onCall({ cors: true }, async (request) => {
    // Admin kontrolü
    if (!request.auth) throw new HttpsError("unauthenticated", "Yetki yok.");
    const adminDoc = await db.collection("users").doc(request.auth.uid).get();
    if (!adminDoc.data()?.isAdmin) throw new HttpsError("permission-denied", "Sadece adminler mesaj gönderebilir.");

    const { userId, title, message } = request.data;
    if (!userId || !title || !message) throw new HttpsError("invalid-argument", "Eksik bilgi.");

    // Bildirim geçmişine kaydet
    await db.collection("users").doc(userId).collection("notifications").add({
        title,
        body: message,
        type: "ADMIN_MESSAGE",
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        isRead: false
    });

    // Push Bildirim Gönder
    await sendNotification(userId, title, message, { type: "ADMIN_MESSAGE" });

    return { success: true };
});

// ==================== AGGREGATION SYSTEM (COST OPTIMIZATION) ====================

/**
 * Kullanıcı sayısı takibi
 */
export const onUserCreated = functionsV1.auth.user().onCreate(async (user) => {
    console.log(`[Aggregation] New user created: ${user.uid}`);
    await db.collection("stats").doc("global").set({
        totalUsers: admin.firestore.FieldValue.increment(1)
    }, { merge: true });
});

/**
 * Finansal metriklerin (Gelir ve Escrow) takibi
 * Transaction bazlı toplama (O(1) Dashboard okuması sağlar)
 */
export const onTransactionCreatedAggregation = functionsV1.firestore
    .document("transactions/{txId}")
    .onCreate(async (snapshot) => {
        const tx = snapshot.data();
        const statsRef = db.collection("stats").doc("global");

        let revenueChange = 0;
        let escrowChange = 0;

        // 1. Gelir (Revenue) Hesaplama (%8 Depozit Komisyonu)
        if (tx.type === "DEPOSIT" && tx.status === "completed") {
            revenueChange += (tx.amount * 0.08);
        }

        // 2. Para Çekme Ücreti (Sabit 5 TL Gelir)
        if (tx.type === "WITHDRAWAL") {
            // Not: requestWithdrawalV2'de 5 TL kesiliyor, bu bir gelirdir.
            revenueChange += 5.0;
        }

        // 3. Escrow (Blokeli Bakiye) Takibi
        if (tx.type === "TASK_PAYMENT") {
            // Ödeme bloke edildiğinde escrow artar (amount negatiftir, mutlak değer alıyoruz)
            escrowChange += Math.abs(tx.amount);
        } else if (tx.type === "EARNING" || tx.type === "REFUND" || tx.type === "PAYMENT_RECEIVED") {
            // Ödeme serbest bırakıldığında veya iade edildiğinde escrow azalır
            escrowChange -= Math.abs(tx.amount);
        }

        if (revenueChange !== 0 || escrowChange !== 0) {
            console.log(`[Aggregation] Updating stats: Revenue +${revenueChange}, Escrow ${escrowChange}`);
            await statsRef.set({
                totalRevenue: admin.firestore.FieldValue.increment(revenueChange),
                totalEscrow: admin.firestore.FieldValue.increment(escrowChange),
                lastUpdateAt: admin.firestore.FieldValue.serverTimestamp()
            }, { merge: true });
        }
    });

/**
 * Flash Task (Boost) gelir takibi
 */
export const onBoostAggregation = functionsV1.firestore
    .document("transactions/{txId}")
    .onCreate(async (snapshot) => {
        const tx = snapshot.data();
        if (tx.type === "TASK_BOOST") {
            await db.collection("stats").doc("global").set({
                totalRevenue: admin.firestore.FieldValue.increment(10.0), // 10 TL boost ücreti
                lastUpdateAt: admin.firestore.FieldValue.serverTimestamp()
            }, { merge: true });
        }
    });

/**
 * Kullanıcı durumunu güncelle (Hesabı askıya al / aktif et)
 */
/**
 * Kullanıcı durumunu güncelle (Hesabı askıya al / aktif et)
 */
export const updateUserStatusV2 = onCall({ cors: true }, async (request) => {
    // Admin kontrolü
    if (!request.auth) throw new HttpsError("unauthenticated", "Yetki yok.");
    const adminDoc = await db.collection("users").doc(request.auth.uid).get();
    if (!adminDoc.data()?.isAdmin) throw new HttpsError("permission-denied", "Sadece adminler durum güncelleyebilir.");

    const { userId, status } = request.data; // status: "active" | "suspended"
    if (!userId || !status) throw new HttpsError("invalid-argument", "Eksik bilgi.");

    await db.collection("users").doc(userId).update({
        status: status,
        isSuspended: status === "suspended",
        suspendedAt: status === "suspended" ? admin.firestore.FieldValue.serverTimestamp() : null
    });

    if (status === "suspended") {
        await sendNotification(
            userId,
            "⚠️ Hesabınız Askıya Alındı",
            "Hesabınız güvenlik veya kural ihlali nedeniyle geçici olarak askıya alınmıştır. Detaylar için destek ile iletişime geçin."
        );
    }

    return { success: true };
});

// ==================== FLASH TASK (BOOST) ====================

/**
 * Görevi öne çıkar (Radar/Flash Task)
 * 10 TL karşılığında görevi tüm kampüste bildirime sokar ve öne çıkarır.
 */
export const boostTaskV2 = onCall({ cors: true }, async (request) => {
    if (!request.auth) throw new HttpsError("unauthenticated", "Giriş yapmalısınız.");

    const { taskId } = request.data;
    const userId = request.auth.uid;
    const boostFee = 10.0;

    if (!taskId) throw new HttpsError("invalid-argument", "Görev ID eksik.");

    try {
        return await db.runTransaction(async (transaction) => {
            const userRef = db.collection("users").doc(userId);
            const taskRef = db.collection("tasks").doc(taskId);

            const userDoc = await transaction.get(userRef);
            const taskDoc = await transaction.get(taskRef);

            if (!userDoc.exists) throw new Error("Kullanıcı bulunamadı.");
            if (!taskDoc.exists) throw new Error("Görev bulunamadı.");

            const userData = userDoc.data()!;
            const taskData = taskDoc.data()!;

            if (taskData.creatorId !== userId) throw new Error("Sadece görev sahibi öne çıkarabilir.");
            if (taskData.isBoosted) throw new Error("Görev zaten öne çıkarılmış.");
            if ((userData.balance || 0) < boostFee) throw new Error("Yetersiz bakiye. Öne çıkarma ücreti 10 TL'dir.");

            // 1. Bakiyeyi düş
            transaction.update(userRef, {
                balance: admin.firestore.FieldValue.increment(-boostFee)
            });

            // 2. Görevi güncelle
            transaction.update(taskRef, {
                isBoosted: true,
                boostedAt: admin.firestore.FieldValue.serverTimestamp(),
                radarStatus: "ACTIVE"
            });

            // 3. İşlem kaydı
            const txRef = db.collection("transactions").doc();
            transaction.set(txRef, {
                userId,
                type: "TASK_BOOST",
                amount: -boostFee,
                description: `Görevi Öne Çıkarma: ${taskData.title}`,
                taskId,
                createdAt: admin.firestore.FieldValue.serverTimestamp(),
                status: "completed"
            });

            return { success: true, message: "Görev başarıyla öne çıkarıldı! 🚀" };
        });
    } catch (error: any) {
        throw new HttpsError("internal", error.message || "Bir hata oluştu.");
    }
});
// ==================== ADMIN 2FA SİSTEMİ ====================

/**
 * Admin girişi için 6 haneli 2FA kodu üret ve e-posta olarak (simüle) gönder
 */
export const sendAdmin2FACode = onCall({ cors: true, region: "us-central1" }, async (request) => {
    if (!request.auth) throw new HttpsError("unauthenticated", "Yetki yok.");

    const userId = request.auth.uid;
    const userDoc = await db.collection("users").doc(userId).get();

    if (!userDoc.data()?.isAdmin) {
        throw new HttpsError("permission-denied", "Sadece adminler 2FA kodu alabilir.");
    }

    const code = Math.floor(100000 + Math.random() * 900000).toString();
    const expiresAt = admin.firestore.Timestamp.fromMillis(Date.now() + 15 * 60 * 1000); // 15 Dakika geçerli

    await db.collection("admin_2fa").doc(userId).set({
        code,
        expiresAt,
        createdAt: admin.firestore.FieldValue.serverTimestamp()
    });

    // Email gönderimi (Nodemailer)
    const mailOptions = {
        from: `"AcilNakit Admin" <${process.env.EMAIL_USER}>`,
        to: userDoc.data()?.email || request.auth.token.email,
        subject: "AcilNakit Admin - Giriş Güvenlik Kodu",
        html: `
            <div style="font-family: sans-serif; padding: 20px; color: #333;">
                <h2>Güvenlik Doğrulaması</h2>
                <p>Admin paneline erişmek için aşağıdaki güvenlik kodunu kullanın:</p>
                <div style="background: #f4f4f4; padding: 20px; text-align: center; border-radius: 10px;">
                    <h1 style="font-size: 40px; letter-spacing: 10px; margin: 0; color: #0038A8;">${code}</h1>
                </div>
                <p style="font-size: 12px; color: #999; margin-top: 20px;">Bu kod 15 dakika içinde geçerliliğini yitirecektir. Eğer bu işlemi siz yapmadıysanız lütfen şifrenizi değiştirin.</p>
            </div>
        `
    };

    try {
        await transporter.sendMail(mailOptions);
        console.log(`Email sent successfully to ${mailOptions.to}`);
    } catch (error) {
        console.error("Nodemailer error:", error);
        // Hata olsa bile dokümanı oluşturduk, manuel bakılabilir
    }

    return { success: true, message: "Kod e-postanıza gönderildi." };
});

/**
 * 2FA kodunu doğrula
 */
export const verifyAdmin2FACode = onCall({ cors: true, region: "us-central1" }, async (request) => {
    if (!request.auth) throw new HttpsError("unauthenticated", "Yetki yok.");

    const { code } = request.data;
    const userId = request.auth.uid;

    if (!code) throw new HttpsError("invalid-argument", "Kod gerekli.");

    const codeDoc = await db.collection("admin_2fa").doc(userId).get();

    if (!codeDoc.exists) throw new HttpsError("not-found", "Kod bulunamadı.");
    const codeData = codeDoc.data()!;

    if (codeData.code !== code) throw new HttpsError("invalid-argument", "Hatalı kod.");
    if (codeData.expiresAt.toMillis() < Date.now()) throw new HttpsError("deadline-exceeded", "Kodun süresi dolmuş.");

    // Başarılı doğrulamadan sonra kodu sil
    await codeDoc.ref.delete();

    // Opsiyonel: Admin belgesine "lastVerifiedAt" ekle
    await db.collection("users").doc(userId).update({
        last2FAVerifiedAt: admin.firestore.FieldValue.serverTimestamp()
    });

    return { success: true };
});
