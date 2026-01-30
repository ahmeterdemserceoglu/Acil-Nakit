"use client";

import { useEffect, useState } from "react";
import {
    AlertTriangle,
    Gavel,
    User,
    MessageSquare,
    Clock,
    CheckCircle2,
    XCircle,
    ArrowRight,
    Scale,
    ShieldAlert,
    Info,
    ChevronRight,
    Search
} from "lucide-react";
import { db } from "@/lib/firebase";
import {
    collection,
    query,
    where,
    getDocs,
    doc,
    updateDoc,
    serverTimestamp,
    getDoc,
    orderBy,
    limit,
    onSnapshot
} from "firebase/firestore";

interface Dispute {
    id: string;
    taskId: string;
    reporterId: string;
    reportedUserId: string;
    reason: string;
    description: string;
    status: string;
    createdAt: any;
    reporterName?: string;
    reportedName?: string;
}

interface Message {
    id: string;
    senderId: string;
    text: string;
    timestamp: any;
}

export default function DisputesPage() {
    const [disputes, setDisputes] = useState<Dispute[]>([]);
    const [loading, setLoading] = useState(true);
    const [selectedDispute, setSelectedDispute] = useState<Dispute | null>(null);
    const [task, setTask] = useState<any>(null);
    const [messages, setMessages] = useState<Message[]>([]);
    const [actionLoading, setActionLoading] = useState(false);

    useEffect(() => {
        const q = query(collection(db, "disputes"), orderBy("createdAt", "desc"));
        const unsubscribe = onSnapshot(q, (snap) => {
            const items = snap.docs.map(doc => ({ id: doc.id, ...doc.data() } as Dispute));
            setDisputes(items);
            setLoading(false);
        });
        return () => unsubscribe();
    }, []);

    useEffect(() => {
        if (selectedDispute) {
            fetchDisputeDetails(selectedDispute);
        } else {
            setTask(null);
            setMessages([]);
        }
    }, [selectedDispute]);

    async function fetchDisputeDetails(dispute: Dispute) {
        const taskSnap = await getDoc(doc(db, "tasks", dispute.taskId));
        if (taskSnap.exists()) {
            setTask({ id: taskSnap.id, ...taskSnap.data() });
        }

        if (!dispute.reporterName) {
            const reporterSnap = await getDoc(doc(db, "users", dispute.reporterId));
            const reportedSnap = await getDoc(doc(db, "users", dispute.reportedUserId));

            setDisputes(prev => prev.map(d => d.id === dispute.id ? {
                ...d,
                reporterName: reporterSnap.data()?.name || "Bilinmiyor",
                reportedName: reportedSnap.data()?.name || "Bilinmiyor"
            } : d));
        }

        const chatQ = query(collection(db, "chats"), where("taskId", "==", dispute.taskId), limit(1));
        const chatSnap = await getDocs(chatQ);
        if (!chatSnap.empty) {
            const chatId = chatSnap.docs[0].id;
            const msgQ = query(collection(db, "chats", chatId, "messages"), orderBy("timestamp", "asc"));
            const msgSnap = await getDocs(msgQ);
            setMessages(msgSnap.docs.map(d => ({ id: d.id, ...d.data() } as Message)));
        } else {
            setMessages([]);
        }
    }

    async function resolve(resolution: "RESOLVED_CREATOR_WIN" | "RESOLVED_WORKER_WIN") {
        if (!selectedDispute) return;
        if (!confirm("Bu kararı onaylıyor musunuz? İşlem geri alınamaz.")) return;

        setActionLoading(true);
        try {
            await updateDoc(doc(db, "disputes", selectedDispute.id), {
                status: resolution,
                resolvedAt: serverTimestamp()
            });
            setSelectedDispute(null);
        } catch (e) {
            console.error(e);
            alert("İşlem başarısız.");
        }
        setActionLoading(false);
    }

    return (
        <div className="flex h-[calc(100vh-120px)] gap-8 py-4 animate-in fade-in duration-700">
            {/* Case List */}
            <div className="w-96 flex flex-col space-y-6">
                <div>
                    <h2 className="text-2xl font-bold text-gray-900 tracking-tight">Vaka Listesi</h2>
                    <p className="text-xs font-semibold text-gray-400 mt-1 uppercase tracking-widest">Bekleyen Uyuşmazlıklar</p>
                </div>

                <div className="flex-1 overflow-y-auto space-y-3 pr-2 custom-scrollbar">
                    {loading ? (
                        <div className="space-y-3">
                            {[1, 2, 3].map(i => <div key={i} className="h-32 bg-gray-100/50 rounded-2xl animate-pulse" />)}
                        </div>
                    ) : disputes.length === 0 ? (
                        <div className="mica-panel p-10 rounded-[2rem] text-center">
                            <CheckCircle2 size={32} className="mx-auto text-green-200 mb-3" />
                            <p className="text-xs font-bold text-gray-400">Aktif vaka kaydı bulunmuyor.</p>
                        </div>
                    ) : (
                        disputes.map((d) => (
                            <div
                                key={d.id}
                                onClick={() => setSelectedDispute(d)}
                                className={`mica-panel p-5 rounded-2xl cursor-pointer transition-all border-l-4 ${selectedDispute?.id === d.id
                                    ? "border-l-[var(--isbank-blue)] bg-white ring-1 ring-gray-100 shadow-xl"
                                    : "border-l-gray-200 hover:border-l-gray-300 bg-white/50"
                                    }`}
                            >
                                <div className="flex justify-between items-center mb-2">
                                    <span className={`text-[9px] font-bold px-2 py-0.5 rounded-md uppercase tracking-widest ${d.status === "PENDING" ? "bg-red-50 text-red-500" : "bg-green-50 text-green-500"
                                        }`}>
                                        {d.status === "PENDING" ? "Açık Dava" : "Kapatıldı"}
                                    </span>
                                    <span className="text-[9px] font-bold text-gray-400 flex items-center gap-1">
                                        <Clock size={10} /> {d.createdAt?.toDate()?.toLocaleDateString('tr-TR')}
                                    </span>
                                </div>
                                <h3 className="font-bold text-gray-800 text-sm truncate mb-1">{d.reason}</h3>
                                <p className="text-[11px] font-medium text-gray-500 line-clamp-1">{d.description}</p>
                            </div>
                        ))
                    )}
                </div>
            </div>

            {/* Case Workspace */}
            <div className="flex-1">
                {selectedDispute ? (
                    <div className="h-full mica-panel rounded-[2rem] flex flex-col overflow-hidden border border-gray-100 shadow-2xl">
                        <header className="p-8 border-b border-gray-100 bg-white/50 flex items-center justify-between">
                            <div className="flex items-center gap-4">
                                <div className="w-12 h-12 bg-red-50 text-red-600 rounded-2xl flex items-center justify-center">
                                    <ShieldAlert size={24} />
                                </div>
                                <div>
                                    <h3 className="text-lg font-bold text-gray-900">{selectedDispute.reason}</h3>
                                    <p className="text-[10px] font-bold text-gray-400 uppercase tracking-widest mt-0.5">Vaka: {selectedDispute.id}</p>
                                </div>
                            </div>
                            <div className="px-4 py-2 bg-gray-100 rounded-xl text-[10px] font-bold text-gray-500">
                                GÖREV: #{selectedDispute.taskId.slice(-6)}
                            </div>
                        </header>

                        <div className="flex-1 overflow-y-auto p-8 space-y-8 custom-scrollbar">
                            <div className="grid grid-cols-2 gap-4">
                                <PartyInfo label="Davacı" name={selectedDispute.reporterName} isReporter />
                                <PartyInfo label="Davalı" name={selectedDispute.reportedName} />
                            </div>

                            <div className="bg-gray-50 p-6 rounded-2xl border border-gray-100">
                                <h4 className="text-[10px] font-bold text-gray-400 uppercase tracking-widest mb-2 flex items-center gap-2">
                                    <Info size={12} /> Vaka Beyanı
                                </h4>
                                <p className="text-sm font-medium text-gray-700 leading-relaxed">{selectedDispute.description}</p>
                            </div>

                            {task && (
                                <div className="mica-panel p-6 rounded-2xl border border-gray-100 flex items-center justify-between bg-[var(--isbank-blue)] text-white group">
                                    <div className="flex items-center gap-4">
                                        <div className="w-10 h-10 bg-white/20 rounded-xl flex items-center justify-center">
                                            <Scale size={18} />
                                        </div>
                                        <div>
                                            <p className="text-[10px] font-bold opacity-60 uppercase tracking-widest">İhtilaflı Ödeme</p>
                                            <h5 className="text-md font-bold">{task.title}</h5>
                                        </div>
                                    </div>
                                    <div className="text-right">
                                        <p className="text-2xl font-bold tracking-tighter">₺{task.rewardAmount}</p>
                                        <p className="text-[9px] font-bold opacity-60 uppercase tracking-widest">Emanette</p>
                                    </div>
                                </div>
                            )}

                            <div className="space-y-4">
                                <h4 className="text-[10px] font-bold text-gray-400 uppercase tracking-widest flex items-center gap-2">
                                    <MessageSquare size={12} /> Sohbet Kanıtları
                                </h4>
                                <div className="bg-white rounded-2xl border border-gray-100 p-6 min-h-[200px] space-y-3">
                                    {messages.length === 0 ? (
                                        <div className="text-center py-10 text-gray-300 text-xs italic">Sohbet kaydı bulunamadı.</div>
                                    ) : (
                                        messages.map((m) => (
                                            <div key={m.id} className={`flex ${m.senderId === selectedDispute.reporterId ? 'justify-start' : 'justify-end'}`}>
                                                <div className={`max-w-[80%] px-4 py-2.5 rounded-2xl text-[13px] font-medium ${m.senderId === selectedDispute.reporterId
                                                        ? 'bg-blue-50 text-blue-800'
                                                        : 'bg-gray-50 text-gray-700'
                                                    }`}>
                                                    {m.text}
                                                </div>
                                            </div>
                                        ))
                                    )}
                                </div>
                            </div>
                        </div>

                        {selectedDispute.status === "PENDING" && (
                            <footer className="p-8 border-t border-gray-100 bg-white/50 flex gap-4">
                                <button
                                    onClick={() => resolve("RESOLVED_CREATOR_WIN")}
                                    disabled={actionLoading}
                                    className="flex-1 py-4 border border-red-100 bg-red-50 text-red-600 rounded-xl font-bold text-xs uppercase tracking-widest hover:bg-red-600 hover:text-white transition-all"
                                >
                                    {actionLoading ? "..." : "İptal ve İade"}
                                </button>
                                <button
                                    onClick={() => resolve("RESOLVED_WORKER_WIN")}
                                    disabled={actionLoading}
                                    className="flex-1 py-4 bg-green-600 text-white rounded-xl font-bold text-xs uppercase tracking-widest shadow-lg shadow-green-100 hover:bg-green-700 transition-all"
                                >
                                    {actionLoading ? "..." : "Onay ve Ödeme"}
                                </button>
                            </footer>
                        )}

                        {selectedDispute.status.startsWith("RESOLVED") && (
                            <footer className="p-6 bg-gray-50 text-center font-bold text-[10px] text-gray-400 uppercase tracking-widest">
                                VAKA ÇÖZÜMLENDİ VE ARŞİVLENDİ
                            </footer>
                        )}
                    </div>
                ) : (
                    <div className="h-full mica-panel rounded-[2rem] flex flex-col items-center justify-center text-gray-300 border-2 border-dashed border-gray-100">
                        <Gavel size={64} strokeWidth={1} className="opacity-10 mb-4" />
                        <p className="text-sm font-semibold">Bir dosya seçerek inceleme başlatın</p>
                    </div>
                )}
            </div>
        </div>
    );
}

function PartyInfo({ label, name, isReporter }: any) {
    return (
        <div className="mica-panel p-5 rounded-xl border border-gray-100 flex items-center gap-3 bg-white">
            <div className={`w-10 h-10 rounded-xl flex items-center justify-center ${isReporter ? 'bg-blue-50 text-blue-600' : 'bg-indigo-50 text-indigo-600'}`}>
                <User size={18} />
            </div>
            <div>
                <p className="text-[9px] font-bold text-gray-400 uppercase tracking-widest">{label}</p>
                <p className="text-sm font-bold text-gray-800">{name || "Yükleniyor..."}</p>
            </div>
        </div>
    );
}
