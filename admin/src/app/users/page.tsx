"use client";

import { useEffect, useState } from "react";
// Re-importing correct icons from lucide-react (correcting my typo)
import * as Lucide from "lucide-react";
import { db, functions } from "@/lib/firebase";
import { httpsCallable } from "firebase/functions";
import { collection, getDocs, query, orderBy, Timestamp } from "firebase/firestore";

interface UserProfile {
    id: string;
    name: string;
    email: string;
    balance: number;
    isVerified: boolean;
    createdAt?: Timestamp;
    photoUrl?: string;
    phone?: string;
    campusName?: string;
    department?: string;
    trustScore?: number;
    status?: string;
    isSuspended?: boolean;
}

export default function UsersPage() {
    const [users, setUsers] = useState<UserProfile[]>([]);
    const [loading, setLoading] = useState(true);
    const [searchTerm, setSearchTerm] = useState("");
    const [filter, setFilter] = useState("all");
    const [selectedUser, setSelectedUser] = useState<UserProfile | null>(null);
    const [isMessageModalOpen, setIsMessageModalOpen] = useState(false);
    const [isSuspendModalOpen, setIsSuspendModalOpen] = useState(false);
    const [adminMessage, setAdminMessage] = useState("");
    const [actionLoading, setActionLoading] = useState(false);

    useEffect(() => {
        async function fetchUsers() {
            setLoading(true);
            try {
                const q = query(collection(db, "users"), orderBy("createdAt", "desc"));
                const querySnapshot = await getDocs(q);
                const usersList: UserProfile[] = [];
                querySnapshot.forEach((doc) => {
                    usersList.push({ id: doc.id, ...doc.data() } as UserProfile);
                });
                setUsers(usersList);
            } catch (error) {
                console.error("Error fetching users:", error);
            }
            setLoading(false);
        }
        fetchUsers();
    }, []);

    const filteredUsers = users.filter((u) => {
        const matchesSearch =
            u.name?.toLowerCase().includes(searchTerm.toLowerCase()) ||
            u.email?.toLowerCase().includes(searchTerm.toLowerCase());

        if (filter === "all") return matchesSearch;
        if (filter === "verified") return matchesSearch && u.isVerified;
        if (filter === "unverified") return matchesSearch && !u.isVerified;
        return matchesSearch;
    });

    const handleSendMessage = async () => {
        if (!selectedUser || !adminMessage) return;
        setActionLoading(true);
        try {
            const sendMsg = httpsCallable(functions, 'sendAdminMessageV2');
            await sendMsg({
                userId: selectedUser.id,
                title: "📣 AcilNakit Bildirimi",
                message: adminMessage
            });
            alert("Mesaj başarıyla gönderildi.");
            setIsMessageModalOpen(false);
            setAdminMessage("");
        } catch (e) {
            console.error(e);
            alert("Hata oluştu.");
        }
        setActionLoading(false);
    };

    const handleToggleSuspension = async () => {
        if (!selectedUser) return;
        setActionLoading(true);
        try {
            const newStatus = selectedUser.isSuspended ? "active" : "suspended";
            const updateStatus = httpsCallable(functions, 'updateUserStatusV2');
            await updateStatus({
                userId: selectedUser.id,
                status: newStatus
            });
            alert(`Kullanıcı durumu '${newStatus}' olarak güncellendi.`);
            setIsSuspendModalOpen(false);
            setUsers(users.map(u => u.id === selectedUser.id ? { ...u, status: newStatus, isSuspended: newStatus === "suspended" } : u));
            setSelectedUser(null);
        } catch (e) {
            console.error(e);
            alert("Hata oluştu.");
        }
        setActionLoading(false);
    };

    return (
        <div className="max-w-[1600px] mx-auto space-y-8 py-4 animate-in fade-in duration-700">
            <div className="flex flex-col md:flex-row md:items-end justify-between gap-6">
                <div>
                    <h2 className="text-3xl font-bold text-gray-900 tracking-tight">Kullanıcı Yönetimi</h2>
                    <p className="text-gray-500 mt-1 font-medium">Sistemdeki tüm kayıtlı aktörleri yönetin.</p>
                </div>

                <div className="flex items-center gap-3">
                    <div className="relative">
                        <Lucide.Search className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" size={16} />
                        <input
                            type="text"
                            placeholder="İsim veya email ile ara..."
                            className="pl-11 pr-4 py-2.5 rounded-xl border border-gray-200 outline-none w-full md:w-80 transition-all focus:ring-2 focus:ring-blue-100 focus:border-[var(--isbank-blue)] bg-white text-sm font-medium"
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                        />
                    </div>
                    <select
                        className="bg-white border border-gray-200 rounded-xl px-4 py-2.5 text-xs font-bold text-gray-600 outline-none hover:bg-gray-50 transition-all cursor-pointer"
                        value={filter}
                        onChange={(e) => setFilter(e.target.value)}
                    >
                        <option value="all">Tüm Üyeler</option>
                        <option value="verified">Sadece Onaylılar</option>
                        <option value="unverified">Onay Bekleyenler</option>
                    </select>
                </div>
            </div>

            <div className="mica-panel rounded-[2rem] overflow-hidden border border-gray-100 shadow-sm">
                {loading ? (
                    <div className="py-24 flex flex-col items-center justify-center text-gray-400">
                        <div className="w-8 h-8 border-2 border-[var(--isbank-blue)] border-t-transparent rounded-full animate-spin"></div>
                    </div>
                ) : filteredUsers.length === 0 ? (
                    <div className="py-24 text-center">
                        <Lucide.Users size={48} className="mx-auto text-gray-200 mb-4" />
                        <p className="font-semibold text-gray-500">Kullanıcı bulunamadı.</p>
                    </div>
                ) : (
                    <table className="w-full text-left">
                        <thead>
                            <tr className="bg-gray-50 shadow-inner">
                                <th className="px-8 py-5 text-[10px] font-bold text-gray-400 uppercase tracking-widest border-b border-gray-100">Kullanıcı</th>
                                <th className="px-8 py-5 text-[10px] font-bold text-gray-400 uppercase tracking-widest border-b border-gray-100">Kampüs / Bölüm</th>
                                <th className="px-8 py-5 text-[10px] font-bold text-gray-400 uppercase tracking-widest border-b border-gray-100">Güven & Onay</th>
                                <th className="px-8 py-5 text-[10px] font-bold text-gray-400 uppercase tracking-widest border-b border-gray-100">Bakiye</th>
                                <th className="px-8 py-5 text-right border-b border-gray-100"></th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-50">
                            {filteredUsers.map((user) => (
                                <tr key={user.id} className="hover:bg-gray-50/50 transition-colors group">
                                    <td className="px-8 py-5">
                                        <div className="flex items-center gap-4">
                                            <div className="w-11 h-11 rounded-xl bg-gray-100 flex items-center justify-center border border-gray-200 overflow-hidden shrink-0">
                                                {user.photoUrl ? (
                                                    <img src={user.photoUrl} alt="" className="w-full h-full object-cover" />
                                                ) : (
                                                    <span className="text-sm font-bold text-gray-400">
                                                        {user.name?.[0] || 'U'}
                                                    </span>
                                                )}
                                            </div>
                                            <div className="min-w-0">
                                                <p className="font-bold text-gray-800 text-sm">{user.name || "İsimsiz"}</p>
                                                <p className="text-[11px] font-medium text-gray-400 truncate">{user.email}</p>
                                            </div>
                                        </div>
                                    </td>
                                    <td className="px-8 py-5">
                                        <p className="text-xs font-bold text-gray-700">{user.campusName || "Bilinmiyor"}</p>
                                        <p className="text-[10px] font-semibold text-gray-400 uppercase mt-0.5">{user.department || "-"}</p>
                                    </td>
                                    <td className="px-8 py-5">
                                        <div className="flex items-center gap-2">
                                            {user.isVerified ? (
                                                <Lucide.ShieldCheck size={16} className="text-green-500" />
                                            ) : (
                                                <Lucide.ShieldAlert size={16} className="text-orange-400" />
                                            )}
                                            <div className="flex flex-col">
                                                <span className="text-[10px] font-bold text-gray-600">Skor: {user.trustScore || 100}</span>
                                                {user.isSuspended && (
                                                    <span className="text-[9px] font-black text-red-500 uppercase tracking-tighter">ENGELLEDİ</span>
                                                )}
                                            </div>
                                        </div>
                                    </td>
                                    <td className="px-8 py-5">
                                        <span className="font-bold text-gray-900 text-sm">₺{(user.balance || 0).toFixed(2)}</span>
                                    </td>
                                    <td className="px-8 py-5 text-right">
                                        <button
                                            onClick={() => setSelectedUser(user)}
                                            className="p-2 hover:bg-white rounded-lg border border-transparent hover:border-gray-200 transition-all shadow-sm"
                                        >
                                            <Lucide.MoreVertical size={18} className="text-gray-400" />
                                        </button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                )}
            </div>

            {/* Slide-over Detail Panel */}
            {selectedUser && (
                <div className="fixed inset-0 z-[100] flex justify-end">
                    <div className="absolute inset-0 bg-black/20 backdrop-blur-[2px]" onClick={() => setSelectedUser(null)} />
                    <div className="relative w-full max-w-md mica-panel h-full shadow-2xl flex flex-col animate-in slide-in-from-right duration-500">
                        <div className="p-8 border-b border-gray-100 flex items-center justify-between">
                            <h3 className="text-xl font-bold text-gray-800">Profil İncelemesi</h3>
                            <button onClick={() => setSelectedUser(null)} className="p-2 hover:bg-gray-100 rounded-full transition-all">
                                <Lucide.XCircle size={24} className="text-gray-300 hover:text-red-500" />
                            </button>
                        </div>

                        <div className="flex-1 overflow-y-auto p-8 space-y-10 custom-scrollbar">
                            <div className="flex flex-col items-center">
                                <div className="w-24 h-24 rounded-[2rem] bg-gray-50 border-4 border-white shadow-xl flex items-center justify-center overflow-hidden mb-5">
                                    {selectedUser.photoUrl ? (
                                        <img src={selectedUser.photoUrl} alt="" className="w-full h-full object-cover" />
                                    ) : (
                                        <span className="text-4xl font-bold text-gray-200">{selectedUser.name?.[0]}</span>
                                    )}
                                </div>
                                <h4 className="text-2xl font-bold text-gray-900">{selectedUser.name}</h4>
                                <p className="text-xs font-bold text-gray-400 bg-gray-100 px-3 py-1 rounded-full mt-2 tracking-widest">{selectedUser.id}</p>
                            </div>

                            <div className="grid grid-cols-2 gap-4">
                                <div className="p-5 rounded-2xl bg-white border border-gray-100 shadow-sm">
                                    <p className="text-[10px] font-bold text-gray-400 uppercase tracking-widest mb-1">Cüzdan</p>
                                    <p className="text-xl font-bold text-gray-900">₺{selectedUser.balance.toFixed(2)}</p>
                                </div>
                                <div className="p-5 rounded-2xl bg-white border border-gray-100 shadow-sm">
                                    <p className="text-[10px] font-bold text-gray-400 uppercase tracking-widest mb-1">Güven Skoru</p>
                                    <p className="text-xl font-bold text-[var(--isbank-blue)]">{(selectedUser.trustScore || 100).toFixed(1)}</p>
                                </div>
                            </div>

                            <div className="space-y-4">
                                <DetailItem icon={Lucide.GraduationCap} label="Kampüs" value={selectedUser.campusName} />
                                <DetailItem icon={Lucide.Users} label="Bölüm" value={selectedUser.department} />
                                <DetailItem icon={Lucide.Phone} label="İletişim" value={selectedUser.phone || "Gizli"} />
                                <DetailItem icon={Lucide.Mail} label="E-Posta" value={selectedUser.email} />
                            </div>
                        </div>

                        <div className="p-8 border-t border-gray-100 bg-white/50 space-y-3">
                            <button
                                onClick={() => setIsMessageModalOpen(true)}
                                className="w-full py-4 bg-[var(--isbank-blue)] text-white rounded-xl font-bold text-xs uppercase tracking-widest shadow-lg shadow-blue-100 hover:-translate-y-0.5 transition-all"
                            >
                                Bildirim Mesajı Gönder
                            </button>
                            <button
                                onClick={() => setIsSuspendModalOpen(true)}
                                className={`w-full py-4 rounded-xl font-bold text-xs uppercase tracking-widest transition-all ${selectedUser.isSuspended
                                    ? "bg-green-50 text-green-600 border border-green-100 hover:bg-green-600 hover:text-white"
                                    : "bg-red-50 text-red-500 border border-red-100 hover:bg-red-500 hover:text-white text-red-600"
                                    }`}
                            >
                                {selectedUser.isSuspended ? "Kısıtlamayı Kaldır" : "Hesabı Askıya Al"}
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Modal Components (Optimized Designs) */}
            {isMessageModalOpen && (
                <div className="fixed inset-0 z-[110] flex items-center justify-center p-4 overflow-hidden">
                    <div className="absolute inset-0 bg-black/30 backdrop-blur-sm" onClick={() => setIsMessageModalOpen(false)} />
                    <div className="relative w-full max-w-lg mica-panel rounded-[2.5rem] p-10 animate-in zoom-in-95 duration-300">
                        <h3 className="text-2xl font-bold text-gray-800 mb-6">Sistem Mesajı</h3>
                        <textarea
                            className="fluent-input h-44 rounded-2xl resize-none p-5 text-sm font-medium"
                            placeholder="Kullanıcıya iletmek istediğiniz not..."
                            value={adminMessage}
                            onChange={(e) => setAdminMessage(e.target.value)}
                        />
                        <div className="mt-8 flex gap-3">
                            <button onClick={() => setIsMessageModalOpen(false)} className="flex-1 py-4 font-bold text-xs text-gray-400 hover:bg-gray-100 rounded-xl transition-all">İPTAL</button>
                            <button
                                onClick={handleSendMessage}
                                disabled={actionLoading || !adminMessage}
                                className="flex-[2] py-4 bg-[var(--isbank-blue)] text-white rounded-xl font-bold text-xs shadow-xl shadow-blue-100 disabled:opacity-50"
                            >
                                {actionLoading ? "..." : "MESAJI GÖNDER"}
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Suspend Confirmation optimized */}
            {isSuspendModalOpen && (
                <div className="fixed inset-0 z-[110] flex items-center justify-center p-4">
                    <div className="absolute inset-0 bg-black/30 backdrop-blur-sm" onClick={() => setIsSuspendModalOpen(false)} />
                    <div className="relative w-full max-w-sm mica-panel rounded-[2rem] p-10 text-center">
                        <div className="w-16 h-16 rounded-full bg-red-50 text-red-500 flex items-center justify-center mx-auto mb-6">
                            <Lucide.AlertOctagon size={32} />
                        </div>
                        <h3 className="text-xl font-bold text-gray-800 mb-2">Emin misiniz?</h3>
                        <p className="text-sm font-medium text-gray-400 mb-8">Bu işlem kullanıcının platform erişimini sınırlayacaktır.</p>
                        <div className="flex gap-3">
                            <button onClick={() => setIsSuspendModalOpen(false)} className="flex-1 py-3 text-xs font-bold text-gray-400">VAZGEÇ</button>
                            <button
                                onClick={handleToggleSuspension}
                                className="flex-[1.5] py-3 bg-red-600 text-white rounded-xl text-xs font-bold shadow-xl shadow-red-100"
                            >
                                ONAYLA
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}

function DetailItem({ icon: Icon, label, value }: any) {
    return (
        <div className="flex items-center gap-4 group">
            <div className="w-10 h-10 rounded-xl bg-gray-50 flex items-center justify-center text-gray-400 border border-gray-100 group-hover:bg-[var(--isbank-blue)] group-hover:text-white transition-all">
                <Icon size={18} />
            </div>
            <div>
                <p className="text-[10px] font-bold text-gray-400 uppercase tracking-widest leading-none mb-1">{label}</p>
                <p className="text-sm font-bold text-gray-800">{value || "-"}</p>
            </div>
        </div>
    );
}
