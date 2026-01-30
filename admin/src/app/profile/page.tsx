"use client";

import { useEffect, useState } from "react";
import { auth, db } from "@/lib/firebase";
import { doc, getDoc, updateDoc, collection, query, where, getDocs } from "firebase/firestore";
import {
    User,
    Mail,
    Shield,
    Calendar,
    Edit2,
    Save,
    LogOut,
    CheckCircle2,
    Clock,
    Activity
} from "lucide-react";
import { motion } from "framer-motion";

const containerVariants = {
    hidden: { opacity: 0 },
    visible: { opacity: 1, transition: { staggerChildren: 0.1 } }
} as const;

const itemVariants = {
    hidden: { y: 20, opacity: 0 },
    visible: { y: 0, opacity: 1 }
} as const;

export default function ProfilePage() {
    const [user, setUser] = useState<any>(null);
    const [stats, setStats] = useState({
        totalApprovals: 0,
        resolvedDisputes: 0,
        securityScore: 85,
        creationDate: "..."
    });
    const [loading, setLoading] = useState(true);
    const [editing, setEditing] = useState(false);
    const [newName, setNewName] = useState("");
    const [status, setStatus] = useState<{ type: 'success' | 'error', msg: string } | null>(null);

    const toggleSecurity = async (field: string, current: boolean) => {
        try {
            const updates: any = { [field]: !current };

            // Eğer IP kısıtlaması açılıyorsa, mevcut IP'yi kaydet
            if (field === 'ipRestriction' && !current) {
                const ipRes = await fetch('https://api.ipify.org?format=json');
                const { ip } = await ipRes.json();
                updates.authorizedIp = ip;
            }

            await updateDoc(doc(db, "users", user.id), updates);
            setUser({ ...user, ...updates });
            setStatus({ type: 'success', msg: field === 'ipRestriction' && !current ? "IP kısıtlaması mevcut IP adresinizle aktif edildi." : "Güvenlik ayarı güncellendi." });
            setTimeout(() => setStatus(null), 3000);
        } catch (e) {
            setStatus({ type: 'error', msg: "Ayar güncellenemedi." });
        }
    };

    useEffect(() => {
        const fetchUserData = async () => {
            const currentUser = auth.currentUser;
            if (currentUser) {
                // Fetch User Doc
                const userDoc = await getDoc(doc(db, "users", currentUser.uid));
                if (userDoc.exists()) {
                    const data = userDoc.data();
                    setUser({ id: currentUser.uid, email: currentUser.email, ...data });
                    setNewName(data.name || "");

                    // Real Stats Calculation
                    const verifiedSnap = await getDocs(query(collection(db, "users"), where("isVerified", "==", true)));
                    const resolvedSnap = await getDocs(query(collection(db, "disputes"), where("status", "==", "RESOLVED")));

                    const creationTime = currentUser.metadata.creationTime
                        ? new Date(currentUser.metadata.creationTime).toLocaleDateString('tr-TR', { year: 'numeric', month: 'long' })
                        : "2024";

                    // Security Score Logic (Fake but based on real factors)
                    let score = 70;
                    if (data.twoFactorEnabled) score += 15;
                    if (data.isAdmin) score += 15;

                    setStats({
                        totalApprovals: verifiedSnap.size,
                        resolvedDisputes: resolvedSnap.size,
                        securityScore: score,
                        creationDate: creationTime
                    });
                }
            }
            setLoading(false);
        };
        fetchUserData();
    }, []);

    const handleUpdate = async () => {
        try {
            await updateDoc(doc(db, "users", user.id), {
                name: newName
            });
            setUser({ ...user, name: newName });
            setEditing(false);
            setStatus({ type: 'success', msg: "Profil başarıyla güncellendi." });
            setTimeout(() => setStatus(null), 3000);
        } catch (error) {
            setStatus({ type: 'error', msg: "Güncelleme sırasında bir hata oluştu." });
        }
    };

    if (loading) {
        return (
            <div className="flex items-center justify-center min-h-[60vh]">
                <div className="w-10 h-10 border-4 border-blue-600 border-t-transparent rounded-full animate-spin"></div>
            </div>
        );
    }

    return (
        <motion.div
            initial="hidden"
            animate="visible"
            variants={containerVariants}
            className="max-w-4xl mx-auto space-y-10 py-6"
        >
            <header className="flex items-center justify-between">
                <div>
                    <h2 className="text-3xl font-bold text-gray-900 tracking-tight">Yönetici Profili</h2>
                    <p className="text-sm font-medium text-gray-400 mt-1">Hesap bilgilerinizi ve güvenlik ayarlarınızı yönetin.</p>
                </div>
            </header>

            {status && (
                <div className={`p-4 rounded-2xl flex items-center gap-3 ${status.type === 'success' ? 'bg-green-50 text-green-700' : 'bg-red-50 text-red-700'}`}>
                    <CheckCircle2 size={20} />
                    <p className="text-sm font-bold">{status.msg}</p>
                </div>
            )}

            <div className="grid grid-cols-1 md:grid-cols-12 gap-8">
                {/* Left Side: Main Info */}
                <div className="md:col-span-8 space-y-8">
                    <motion.div variants={itemVariants} className="mica-panel p-10 rounded-[2.5rem] bg-white border border-gray-100 shadow-sm relative overflow-hidden">
                        <div className="absolute top-0 left-0 w-full h-1.5 bg-gradient-to-r from-blue-600 to-indigo-600" />
                        <div className="flex items-start justify-between mb-10">
                            <div className="flex items-center gap-6">
                                <div className="w-24 h-24 rounded-3xl bg-gray-900 flex items-center justify-center text-white text-3xl font-bold shadow-2xl">
                                    {user?.name?.[0] || "A"}
                                </div>
                                <div>
                                    {editing ? (
                                        <input
                                            type="text"
                                            value={newName}
                                            onChange={(e) => setNewName(e.target.value)}
                                            className="text-3xl font-bold text-gray-900 bg-gray-50 border-b-2 border-blue-600 outline-none w-full"
                                        />
                                    ) : (
                                        <h3 className="text-3xl font-bold text-gray-900 tracking-tight">{user?.name}</h3>
                                    )}
                                    <p className="text-blue-600 font-semibold mt-1 uppercase text-xs tracking-widest">Sistem Yöneticisi</p>
                                </div>
                            </div>
                            <button
                                onClick={() => editing ? handleUpdate() : setEditing(true)}
                                className="flex items-center gap-2 px-5 py-2.5 bg-gray-50 hover:bg-gray-100 rounded-xl text-xs font-bold transition-all"
                            >
                                {editing ? <><Save size={16} /> KAYDET</> : <><Edit2 size={16} /> DÜZENLE</>}
                            </button>
                        </div>

                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-8">
                            <InfoItem icon={Mail} label="E-Posta Adresi" value={user?.email} />
                            <InfoItem icon={Shield} label="Yetki Seviyesi" value={user?.isAdmin ? "Tam Yetkili Admin" : "Sınırlı Yetki"} />
                            <InfoItem icon={Clock} label="Son Giriş" value={new Date().toLocaleDateString('tr-TR')} />
                            <InfoItem icon={Calendar} label="Kayıt Tarihi" value={stats.creationDate} />
                            {user?.authorizedIp && (
                                <div className="sm:col-span-2 p-4 bg-blue-50/50 rounded-2xl border border-blue-100 flex items-center justify-between">
                                    <div className="flex items-center gap-3">
                                        <div className="p-2 bg-blue-600 rounded-lg text-white">
                                            <Shield size={14} />
                                        </div>
                                        <p className="text-xs font-bold text-gray-700">Yetkili IP Adresi: <span className="text-blue-600 ml-1">{user.authorizedIp}</span></p>
                                    </div>
                                    <span className="text-[9px] font-bold text-blue-500 uppercase tracking-tighter">Aktif Koruma</span>
                                </div>
                            )}
                        </div>
                    </motion.div>

                    <motion.div variants={itemVariants} className="grid grid-cols-1 sm:grid-cols-3 gap-6">
                        <StatBox icon={Activity} label="Sistem Onayları" value={stats.totalApprovals} />
                        <StatBox icon={CheckCircle2} label="Çözülen Şikayetler" value={stats.resolvedDisputes} />
                        <StatBox icon={Shield} label="Güvenlik Skoru" value={`%${stats.securityScore}`} />
                    </motion.div>
                </div>

                {/* Right Side: Security & Actions */}
                <div className="md:col-span-4 space-y-6">
                    <motion.div variants={itemVariants} className="mica-panel p-8 rounded-3xl bg-white border border-gray-100 shadow-sm">
                        <h4 className="text-sm font-bold text-gray-900 mb-6 flex items-center gap-2 uppercase tracking-wider">
                            <Shield size={16} className="text-blue-500" />
                            Güvenlik Bilgileri
                        </h4>
                        <div className="space-y-4">
                            <SecurityLock label="İki Faktörlü Doğrulama" active={user?.twoFactorEnabled} onToggle={() => toggleSecurity('twoFactorEnabled', user?.twoFactorEnabled)} />
                            <SecurityLock label="IP Kısıtlaması" active={user?.ipRestriction} onToggle={() => toggleSecurity('ipRestriction', user?.ipRestriction)} />
                            <SecurityLock label="Admin Paneli PIN" active={true} onToggle={() => { }} />
                        </div>
                    </motion.div>

                    <button className="w-full flex items-center justify-center gap-2 p-5 bg-red-50 text-red-600 rounded-3xl font-bold hover:bg-red-100 transition-all group">
                        <LogOut size={20} className="group-hover:-translate-x-1 transition-transform" />
                        OTURUMU KAPAT
                    </button>
                </div>
            </div>
        </motion.div>
    );
}

function InfoItem({ icon: Icon, label, value }: any) {
    return (
        <div className="flex items-start gap-3">
            <div className="p-2.5 bg-gray-50 rounded-xl text-gray-400">
                <Icon size={18} />
            </div>
            <div>
                <p className="text-[10px] font-bold text-gray-400 uppercase tracking-widest">{label}</p>
                <p className="text-sm font-bold text-gray-900 mt-0.5">{value}</p>
            </div>
        </div>
    );
}

function StatBox({ icon: Icon, label, value }: any) {
    return (
        <div className="mica-panel p-6 rounded-3xl bg-white border border-gray-100 shadow-sm text-center">
            <div className="w-10 h-10 bg-blue-50 rounded-2xl flex items-center justify-center mx-auto mb-3 text-blue-600">
                <Icon size={20} />
            </div>
            <p className="text-[10px] font-bold text-gray-400 uppercase tracking-widest">{label}</p>
            <h4 className="text-xl font-bold text-gray-900 mt-1">{value}</h4>
        </div>
    );
}

function SecurityLock({ label, active, onToggle }: any) {
    return (
        <div className="flex items-center justify-between p-3 bg-gray-50 rounded-2xl group transition-all">
            <span className="text-[11px] font-bold text-gray-700">{label}</span>
            <button
                onClick={onToggle}
                className={`relative w-10 h-6 rounded-full transition-all duration-300 ${active ? 'bg-green-500 shadow-[0_0_8px_rgba(34,197,94,0.4)]' : 'bg-gray-300'}`}
            >
                <div className={`absolute top-1 w-4 h-4 bg-white rounded-full transition-all duration-300 ${active ? 'left-5' : 'left-1'}`} />
            </button>
        </div>
    );
}
