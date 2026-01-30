"use client";

import { useEffect, useState } from "react";
import { UserCheck, UserX, Search, ShieldCheck, Eye, XCircle, ExternalLink, Image as ImageIcon } from "lucide-react";
import { db } from "@/lib/firebase";
import { collection, query, where, getDocs, doc, updateDoc } from "firebase/firestore";

export default function VerificationPage() {
    const [users, setUsers] = useState<any[]>([]);
    const [loading, setLoading] = useState(true);
    const [selectedUser, setSelectedUser] = useState<any | null>(null);

    useEffect(() => {
        fetchPendingUsers();
    }, []);

    async function fetchPendingUsers() {
        setLoading(true);
        try {
            const q = query(collection(db, "users"), where("verificationStatus", "==", "pending"));
            const snap = await getDocs(q);
            const items = snap.docs.map(doc => ({ id: doc.id, ...doc.data() }));
            setUsers(items);
        } catch (e) {
            console.error(e);
        }
        setLoading(false);
    }

    async function approveUser(userId: string) {
        try {
            await updateDoc(doc(db, "users", userId), {
                isVerified: true,
                verificationStatus: "verified",
                trustScore: 100.0,
                badges: ["verified"]
            });
            setUsers(users.filter(u => u.id !== userId));
            setSelectedUser(null);
        } catch (e) {
            alert("Hata oluştu");
        }
    }

    async function rejectUser(userId: string) {
        if (!confirm("Bu kimlik doğrulama talebini reddetmek istediğinize emin misiniz?")) return;
        try {
            await updateDoc(doc(db, "users", userId), {
                isVerified: false,
                verificationStatus: "rejected"
            });
            setUsers(users.filter(u => u.id !== userId));
            setSelectedUser(null);
        } catch (e) {
            alert("Hata oluştu");
        }
    }

    return (
        <div className="space-y-8">
            <div className="flex justify-between items-end">
                <div>
                    <h2 className="text-3xl font-black text-gray-800">Öğrenci Doğrulama 🛡️</h2>
                    <p className="text-gray-500 mt-2">Mavi Tik bekleyen öğrencileri buradan yönetebilirsin.</p>
                </div>
                <div className="relative">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={18} />
                    <input
                        type="text"
                        placeholder="Kullanıcı ara..."
                        className="pl-10 pr-4 py-2 rounded-2xl border-0 ring-1 ring-gray-200 focus:ring-2 focus:ring-[var(--fluent-blue)] glass-panel outline-none w-64"
                    />
                </div>
            </div>

            <div className="glass-panel rounded-3xl overflow-hidden">
                <table className="w-full text-left">
                    <thead className="bg-gray-50/50 border-b">
                        <tr>
                            <th className="px-6 py-4 font-bold text-gray-600">Öğrenci</th>
                            <th className="px-6 py-4 font-bold text-gray-600">Okul / Bölüm</th>
                            <th className="px-6 py-4 font-bold text-gray-600">Durum</th>
                            <th className="px-6 py-4 font-bold text-gray-600 text-right">İşlemler</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-gray-100">
                        {loading ? (
                            <tr><td colSpan={4} className="px-6 py-10 text-center text-gray-400">Yükleniyor...</td></tr>
                        ) : users.length === 0 ? (
                            <tr><td colSpan={4} className="px-6 py-10 text-center text-gray-400">Bekleyen doğrulama yok. 🎉</td></tr>
                        ) : users.map((user) => (
                            <tr key={user.id} className="hover:bg-white/40 transition-all">
                                <td className="px-6 py-4">
                                    <div className="flex items-center space-x-3">
                                        <div className="w-10 h-10 rounded-full bg-blue-100 flex items-center justify-center font-bold text-blue-700 uppercase">
                                            {user.name?.[0]}
                                        </div>
                                        <div>
                                            <p className="font-bold text-gray-800 capitalize">{user.name}</p>
                                            <p className="text-xs text-gray-500 lowercase">{user.email}</p>
                                        </div>
                                    </div>
                                </td>
                                <td className="px-6 py-4">
                                    <p className="text-sm font-medium text-gray-700">{user.campusName || "Belirtilmemiş"}</p>
                                    <p className="text-xs text-gray-400">{user.department || "Bölüm Yok"}</p>
                                </td>
                                <td className="px-6 py-4">
                                    <span className="px-3 py-1 rounded-full bg-orange-100 text-orange-600 text-[10px] font-black uppercase">BEKLIYOR</span>
                                </td>
                                <td className="px-6 py-4 text-right">
                                    <div className="flex justify-end space-x-2">
                                        <button
                                            onClick={() => setSelectedUser(user)}
                                            className="p-2 bg-blue-50 text-blue-600 rounded-xl hover:bg-blue-600 hover:text-white transition-all shadow-sm"
                                            title="İncele"
                                        >
                                            <Eye size={18} />
                                        </button>
                                        <button
                                            onClick={() => approveUser(user.id)}
                                            className="p-2 bg-green-50 text-green-600 rounded-xl hover:bg-green-600 hover:text-white transition-all shadow-sm"
                                            title="Onayla"
                                        >
                                            <UserCheck size={18} />
                                        </button>
                                        <button
                                            onClick={() => rejectUser(user.id)}
                                            className="p-2 bg-red-50 text-red-600 rounded-xl hover:bg-red-600 hover:text-white transition-all shadow-sm"
                                            title="Reddet"
                                        >
                                            <UserX size={18} />
                                        </button>
                                    </div>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>

            {/* Verification Detail Modal */}
            {selectedUser && (
                <div className="fixed inset-0 z-[100] flex items-center justify-center p-4">
                    <div className="absolute inset-0 bg-black/60 backdrop-blur-md" onClick={() => setSelectedUser(null)} />
                    <div className="relative w-full max-w-4xl bg-white rounded-[2.5rem] shadow-2xl overflow-hidden flex flex-col md:flex-row animate-in zoom-in-95 duration-300">
                        {/* ID Card Display */}
                        <div className="flex-1 bg-gray-900 flex items-center justify-center p-8 min-h-[400px]">
                            {selectedUser.idCardImageUrl ? (
                                <div className="relative group w-full h-full flex items-center justify-center">
                                    <img
                                        src={selectedUser.idCardImageUrl}
                                        alt="Student ID Card"
                                        className="max-w-full max-h-full object-contain rounded-xl shadow-2xl"
                                    />
                                    <a
                                        href={selectedUser.idCardImageUrl}
                                        target="_blank"
                                        className="absolute top-4 right-4 p-3 bg-white/20 hover:bg-white/40 text-white rounded-full backdrop-blur-md transition-all"
                                    >
                                        <ExternalLink size={20} />
                                    </a>
                                </div>
                            ) : (
                                <div className="text-center text-gray-500 space-y-4">
                                    <ImageIcon size={64} className="mx-auto opacity-20" />
                                    <p>Görsel Bulunamadı</p>
                                </div>
                            )}
                        </div>

                        {/* User Info & Actions */}
                        <div className="w-full md:w-80 bg-white p-8 flex flex-col border-l border-gray-100">
                            <div className="flex justify-between items-start mb-8">
                                <h3 className="text-2xl font-black text-gray-800">Doğrula</h3>
                                <button onClick={() => setSelectedUser(null)} className="p-2 hover:bg-gray-100 rounded-full transition-all">
                                    <XCircle size={24} className="text-gray-300 hover:text-red-500" />
                                </button>
                            </div>

                            <div className="space-y-6 flex-1">
                                <div className="flex items-center gap-4">
                                    <div className="w-12 h-12 rounded-2xl bg-blue-50 flex items-center justify-center font-black text-[var(--isbank-blue)]">
                                        {selectedUser.name?.[0]}
                                    </div>
                                    <div>
                                        <p className="font-black text-gray-800 text-lg">{selectedUser.name}</p>
                                        <p className="text-xs text-gray-400 font-bold">{selectedUser.email}</p>
                                    </div>
                                </div>

                                <div className="space-y-4 bg-gray-50 p-6 rounded-3xl">
                                    <div>
                                        <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest">Okul</p>
                                        <p className="text-sm font-bold text-gray-700">{selectedUser.campusName || "-"}</p>
                                    </div>
                                    <div>
                                        <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest">Bölüm</p>
                                        <p className="text-sm font-bold text-gray-700">{selectedUser.department || "-"}</p>
                                    </div>
                                </div>
                            </div>

                            <div className="mt-8 space-y-3">
                                <button
                                    onClick={() => approveUser(selectedUser.id)}
                                    className="w-full py-4 bg-green-600 text-white rounded-2xl font-black shadow-xl shadow-green-100 hover:scale-[1.02] active:scale-[0.98] transition-all"
                                >
                                    ONAYLA (MAVİ TİK)
                                </button>
                                <button
                                    onClick={() => rejectUser(selectedUser.id)}
                                    className="w-full py-4 bg-red-50 text-red-600 rounded-2xl font-black border border-red-100 hover:bg-red-600 hover:text-white transition-all"
                                >
                                    REDDET
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
