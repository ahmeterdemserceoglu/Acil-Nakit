"use client";

import { useEffect, useState } from "react";
import {
    collection,
    query,
    orderBy,
    getDocs,
    doc,
    updateDoc,
    where,
    limit
} from "firebase/firestore";
import { db } from "@/lib/firebase";
import {
    Search,
    Filter,
    MoreVertical,
    MapPin,
    Clock,
    User,
    AlertCircle,
    CheckCircle2,
    XCircle,
    Package,
    ArrowUpDown
} from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";

const containerVariants = {
    hidden: { opacity: 0 },
    visible: {
        opacity: 1,
        transition: { staggerChildren: 0.05 }
    }
} as const;

const itemVariants = {
    hidden: { y: 10, opacity: 0 },
    visible: {
        y: 0,
        opacity: 1
    }
} as const;

export default function TasksPage() {
    const [tasks, setTasks] = useState<any[]>([]);
    const [loading, setLoading] = useState(true);
    const [searchTerm, setSearchTerm] = useState("");
    const [filterStatus, setFilterStatus] = useState("ALL");

    useEffect(() => {
        fetchTasks();
    }, []);

    async function fetchTasks() {
        setLoading(true);
        try {
            const q = query(collection(db, "tasks"), orderBy("createdAt", "desc"), limit(50));
            const snap = await getDocs(q);
            setTasks(snap.docs.map(d => ({ id: d.id, ...d.data() })));
        } catch (error) {
            console.error("Error fetching tasks:", error);
        }
        setLoading(false);
    }

    const filteredTasks = tasks.filter(task => {
        const matchesSearch = task.title?.toLowerCase().includes(searchTerm.toLowerCase()) ||
            task.id?.includes(searchTerm);
        const matchesFilter = filterStatus === "ALL" || task.status === filterStatus;
        return matchesSearch && matchesFilter;
    });

    const getStatusColor = (status: string) => {
        switch (status) {
            case "OPEN": return "bg-blue-50 text-blue-600 border-blue-100";
            case "ASSIGNED": return "bg-orange-50 text-orange-600 border-orange-100";
            case "DELIVERED": return "bg-purple-50 text-purple-600 border-purple-100";
            case "COMPLETED": return "bg-green-50 text-green-600 border-green-100";
            case "CANCELLED": return "bg-red-50 text-red-600 border-red-100";
            default: return "bg-gray-50 text-gray-600 border-gray-100";
        }
    };

    return (
        <motion.div
            initial="hidden"
            animate="visible"
            variants={containerVariants}
            className="space-y-8"
        >
            <header className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div>
                    <h2 className="text-3xl font-bold text-gray-900 tracking-tight">Görev Yönetimi</h2>
                    <p className="text-sm font-medium text-gray-400 mt-1">Sistemdeki tüm görevleri izleyin ve yönetin.</p>
                </div>

                <div className="flex items-center gap-3">
                    <div className="relative">
                        <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={16} />
                        <input
                            type="text"
                            placeholder="Görev Başlığı veya ID..."
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                            className="pl-10 pr-4 py-2 bg-white border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-500/20 transition-all w-64"
                        />
                    </div>
                    <select
                        value={filterStatus}
                        onChange={(e) => setFilterStatus(e.target.value)}
                        className="px-4 py-2 bg-white border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-500/20 transition-all cursor-pointer"
                    >
                        <option value="ALL">Tüm Durumlar</option>
                        <option value="OPEN">Açık</option>
                        <option value="ASSIGNED">Atandı</option>
                        <option value="DELIVERED">Teslim Edildi</option>
                        <option value="COMPLETED">Tamamlandı</option>
                        <option value="CANCELLED">İptal Edildi</option>
                    </select>
                </div>
            </header>

            <div className="mica-panel rounded-[2rem] border border-gray-100 overflow-hidden bg-white shadow-sm">
                <div className="overflow-x-auto">
                    <table className="w-full text-left border-collapse">
                        <thead>
                            <tr className="bg-gray-50/50 border-b border-gray-100">
                                <th className="px-6 py-4 text-[10px] font-bold text-gray-400 uppercase tracking-widest">Görev Bilgisi</th>
                                <th className="px-6 py-4 text-[10px] font-bold text-gray-400 uppercase tracking-widest">Durum</th>
                                <th className="px-6 py-4 text-[10px] font-bold text-gray-400 uppercase tracking-widest">Ödül / Konum</th>
                                <th className="px-6 py-4 text-[10px] font-bold text-gray-400 uppercase tracking-widest">Taraflar</th>
                                <th className="px-6 py-4 text-[10px] font-bold text-gray-400 uppercase tracking-widest text-right">Tarih</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-50">
                            {loading ? (
                                Array(5).fill(0).map((_, i) => (
                                    <tr key={i} className="animate-pulse">
                                        <td colSpan={5} className="px-6 py-8">
                                            <div className="h-4 bg-gray-100 rounded-full w-3/4"></div>
                                        </td>
                                    </tr>
                                ))
                            ) : filteredTasks.length === 0 ? (
                                <tr>
                                    <td colSpan={5} className="px-6 py-12 text-center text-gray-400 font-medium italic">
                                        Eşleşen görev bulunamadı.
                                    </td>
                                </tr>
                            ) : filteredTasks.map((task) => (
                                <motion.tr
                                    key={task.id}
                                    variants={itemVariants}
                                    className="hover:bg-gray-50/50 transition-colors cursor-pointer group"
                                >
                                    <td className="px-6 py-5">
                                        <div className="flex items-center gap-3">
                                            <div className="w-10 h-10 rounded-xl bg-blue-50 flex items-center justify-center text-blue-600 shrink-0">
                                                <Package size={20} />
                                            </div>
                                            <div className="min-w-0">
                                                <p className="text-sm font-bold text-gray-900 truncate uppercase tracking-tight">{task.title}</p>
                                                <p className="text-[10px] font-medium text-gray-400 mt-0.5">ID: {task.id.substring(0, 12)}...</p>
                                            </div>
                                        </div>
                                    </td>
                                    <td className="px-6 py-5">
                                        <span className={`px-3 py-1 rounded-full text-[10px] font-bold border ${getStatusColor(task.status)}`}>
                                            {task.status}
                                        </span>
                                    </td>
                                    <td className="px-6 py-5">
                                        <div className="flex flex-col gap-1">
                                            <p className="text-sm font-bold text-green-600 tracking-tighter">₺{task.rewardAmount}</p>
                                            <div className="flex items-center gap-1 text-[10px] font-semibold text-gray-400">
                                                <MapPin size={10} />
                                                {task.campusName || "Genel"}
                                            </div>
                                        </div>
                                    </td>
                                    <td className="px-6 py-5">
                                        <div className="flex flex-col gap-1.5">
                                            <div className="flex items-center gap-2">
                                                <div className="w-4 h-4 rounded-full bg-gray-100 flex items-center justify-center text-[8px] font-bold text-gray-500 italic">E</div>
                                                <span className="text-[10px] font-bold text-gray-700">İşveren ID: {task.creatorId?.substring(0, 6)}</span>
                                            </div>
                                            {task.workerId && (
                                                <div className="flex items-center gap-2">
                                                    <div className="w-4 h-4 rounded-full bg-blue-100 flex items-center justify-center text-[8px] font-bold text-blue-600 italic">İ</div>
                                                    <span className="text-[10px] font-bold text-gray-700">İşçi ID: {task.workerId.substring(0, 6)}</span>
                                                </div>
                                            )}
                                        </div>
                                    </td>
                                    <td className="px-6 py-5 text-right">
                                        <div className="flex items-center justify-end gap-1 text-[10px] font-bold text-gray-400">
                                            <Clock size={10} />
                                            {task.createdAt?.toDate ? task.createdAt.toDate().toLocaleDateString('tr-TR') : "Yeni"}
                                        </div>
                                    </td>
                                </motion.tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </div>
        </motion.div>
    );
}
