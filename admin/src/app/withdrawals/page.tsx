"use client";

import { useEffect, useState } from "react";
import { CheckCircle, XCircle, Wallet, Copy, ExternalLink, Info } from "lucide-react";
import { db } from "@/lib/firebase";
import { collection, query, where, getDocs, doc, updateDoc, serverTimestamp } from "firebase/firestore";

export default function WithdrawalsPage() {
    const [withdrawals, setWithdrawals] = useState<any[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetchWithdrawals();
    }, []);

    async function fetchWithdrawals() {
        setLoading(true);
        try {
            const q = query(collection(db, "withdrawals"), where("status", "==", "pending"));
            const snap = await getDocs(q);
            const items = snap.docs.map(doc => ({ id: doc.id, ...doc.data() }));
            setWithdrawals(items);
        } catch (e) {
            console.error(e);
        }
        setLoading(false);
    }

    async function completeWithdrawal(id: string) {
        if (!confirm("Banka transferini yaptığınızdan emin misiniz?")) return;
        try {
            await updateDoc(doc(db, "withdrawals", id), {
                status: "completed",
                processedAt: serverTimestamp()
            });
            setWithdrawals(withdrawals.filter(w => w.id !== id));
            alert("İşlem tamamlandı, kullanıcıya bildirim gönderildi.");
        } catch (e) {
            alert("Hata oluştu");
        }
    }

    const copyToClipboard = (text: string) => {
        navigator.clipboard.writeText(text);
        alert("Kopyalandı: " + text);
    };

    return (
        <div className="space-y-8">
            <div>
                <h2 className="text-3xl font-black text-gray-800">Para Çekme Yönetimi 💸</h2>
                <p className="text-gray-500 mt-2">Kullanıcıların harçlıklarını banka hesaplarına aktarma vakti.</p>
            </div>

            <div className="grid grid-cols-1 gap-6">
                {loading ? (
                    <div className="text-center py-20 text-gray-400 font-bold">Talepler yükleniyor...</div>
                ) : withdrawals.length === 0 ? (
                    <div className="glass-panel p-20 rounded-3xl text-center space-y-4">
                        <div className="w-20 h-20 bg-green-100 text-green-600 rounded-full flex items-center justify-center mx-auto">
                            <CheckCircle size={40} />
                        </div>
                        <h3 className="text-xl font-black text-gray-800">Harika! Bekleyen çekim talebi yok.</h3>
                    </div>
                ) : withdrawals.map((w) => (
                    <div key={w.id} className="glass-panel rounded-3xl p-6 flex flex-col md:flex-row items-center justify-between gap-6 hover:shadow-lg transition-all border-l-8 border-l-[var(--isbank-blue)]">
                        <div className="flex-1 space-y-2">
                            <div className="flex items-center space-x-2">
                                <span className="text-2xl font-black text-gray-800">₺{w.amount}</span>
                                <span className="px-2 py-0.5 bg-gray-100 text-[10px] font-bold text-gray-400 rounded">+{w.fee} TL ÜCRET</span>
                            </div>
                            <div className="flex items-center space-x-4">
                                <div className="bg-blue-50 px-3 py-1 rounded-lg flex items-center gap-2 cursor-pointer hover:bg-blue-100 transition-colors" onClick={() => copyToClipboard(w.iban)}>
                                    <p className="text-xs font-mono font-bold text-blue-700">{w.iban}</p>
                                    <Copy size={12} className="text-blue-400" />
                                </div>
                                <p className="text-sm font-black text-gray-700 uppercase tracking-tight">{w.accountName || "İSİM BELİRTİLMEMİŞ"}</p>
                            </div>
                            <div className="flex items-center gap-2 text-[10px] text-gray-400">
                                <Info size={12} />
                                <span>ID: {w.id}</span>
                            </div>
                        </div>

                        <div className="flex items-center space-x-3">
                            <button
                                onClick={() => completeWithdrawal(w.id)}
                                className="flex items-center space-x-2 px-6 py-3 bg-green-600 text-white rounded-2xl font-bold hover:bg-green-700 transition-all shadow-lg shadow-green-200"
                            >
                                <CheckCircle size={20} />
                                <span>TRANSFER YAPILDI</span>
                            </button>
                            <button className="p-3 bg-red-50 text-red-500 rounded-2xl hover:bg-red-500 hover:text-white transition-all">
                                <XCircle size={20} />
                            </button>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
}
