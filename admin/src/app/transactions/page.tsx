"use client";

import { useEffect, useState } from "react";
import {
    collection,
    query,
    orderBy,
    getDocs,
    where,
    limit,
    Timestamp
} from "firebase/firestore";
import { db } from "@/lib/firebase";
import {
    Search,
    Wallet,
    ArrowUpCircle,
    ArrowDownCircle,
    Clock,
    User,
    FileText,
    Calendar,
    Download
} from "lucide-react";
import { motion } from "framer-motion";

const containerVariants = {
    hidden: { opacity: 0 },
    visible: { opacity: 1, transition: { staggerChildren: 0.03 } }
} as const;

const itemVariants = {
    hidden: { y: 10, opacity: 0 },
    visible: { y: 0, opacity: 1 }
} as const;

export default function TransactionsPage() {
    const [transactions, setTransactions] = useState<any[]>([]);
    const [loading, setLoading] = useState(true);
    const [searchTerm, setSearchTerm] = useState("");
    const [filterType, setFilterType] = useState("ALL");

    useEffect(() => {
        fetchTransactions();
    }, []);

    async function fetchTransactions() {
        setLoading(true);
        try {
            const q = query(collection(db, "transactions"), orderBy("createdAt", "desc"), limit(100));
            const snap = await getDocs(q);
            setTransactions(snap.docs.map(d => ({ id: d.id, ...d.data() })));
        } catch (error) {
            console.error("Error fetching transactions:", error);
        }
        setLoading(false);
    }

    const filteredTx = transactions.filter(tx => {
        const matchesSearch = tx.userId?.includes(searchTerm) || tx.orderId?.includes(searchTerm);
        const matchesFilter = filterType === "ALL" || tx.type === filterType;
        return matchesSearch && matchesFilter;
    });

    return (
        <motion.div
            initial="hidden"
            animate="visible"
            variants={containerVariants}
            className="space-y-8"
        >
            <header className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div>
                    <h2 className="text-3xl font-bold text-gray-900 tracking-tight">Finansal İşlemler</h2>
                    <p className="text-sm font-medium text-gray-400 mt-1">Sistemdeki tüm para hareketlerini denetleyin.</p>
                </div>

                <div className="flex items-center gap-3">
                    <div className="relative">
                        <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={16} />
                        <input
                            type="text"
                            placeholder="User ID veya Order ID..."
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                            className="pl-10 pr-4 py-2 bg-white border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-500/20 transition-all w-64"
                        />
                    </div>
                    <select
                        value={filterType}
                        onChange={(e) => setFilterType(e.target.value)}
                        className="px-4 py-2 bg-white border border-gray-200 rounded-xl text-sm cursor-pointer focus:outline-none"
                    >
                        <option value="ALL">Tüm Türler</option>
                        <option value="DEPOSIT">Yükleme (Deposit)</option>
                        <option value="WITHDRAWAL">Para Çekme (Withdrawal)</option>
                        <option value="PAYMENT_RELEASE">Ödeme Serbest</option>
                        <option value="ESCROW_LOCK">Escrow Kilidi</option>
                    </select>
                </div>
            </header>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <SummaryCard title="Toplam İşlem" value={transactions.length} icon={FileText} color="blue" />
                <SummaryCard title="Bugünkü Hacim" value={`₺${transactions.filter(t => t.createdAt?.toDate()?.toLocaleDateString() === new Date().toLocaleDateString()).reduce((acc, curr) => acc + Math.abs(curr.amount || 0), 0)}`} icon={Wallet} color="green" />
                <SummaryCard title="Sistem Geliri" value="Analiz..." icon={ArrowUpCircle} color="indigo" />
            </div>

            <div className="mica-panel rounded-[2rem] border border-gray-100 overflow-hidden bg-white shadow-sm">
                <div className="overflow-x-auto">
                    <table className="w-full text-left border-collapse">
                        <thead>
                            <tr className="bg-gray-50/50 border-b border-gray-100">
                                <th className="px-6 py-4 text-[10px] font-bold text-gray-400 uppercase tracking-widest">Kullanıcı / ID</th>
                                <th className="px-6 py-4 text-[10px] font-bold text-gray-400 uppercase tracking-widest">Tür</th>
                                <th className="px-6 py-4 text-[10px] font-bold text-gray-400 uppercase tracking-widest">Tutar</th>
                                <th className="px-6 py-4 text-[10px] font-bold text-gray-400 uppercase tracking-widest">Durum</th>
                                <th className="px-6 py-4 text-[10px] font-bold text-gray-400 uppercase tracking-widest text-right">Tarih</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-50">
                            {loading ? (
                                Array(5).fill(0).map((_, i) => <tr key={i}><td colSpan={5} className="px-6 py-8 animate-pulse bg-gray-50/20" /></tr>)
                            ) : filteredTx.map((tx) => (
                                <motion.tr
                                    key={tx.id}
                                    variants={itemVariants}
                                    className="hover:bg-gray-50/50 transition-colors"
                                >
                                    <td className="px-6 py-4">
                                        <div className="flex items-center gap-3">
                                            <div className="w-8 h-8 rounded-lg bg-gray-100 flex items-center justify-center text-gray-500 font-bold text-[10px]">
                                                {tx.userId?.substring(0, 2)}
                                            </div>
                                            <div>
                                                <p className="text-xs font-bold text-gray-900">ID: {tx.userId?.substring(0, 8)}...</p>
                                                <p className="text-[9px] font-medium text-gray-400 uppercase tracking-tighter italic">Ref: {tx.orderId || "NA"}</p>
                                            </div>
                                        </div>
                                    </td>
                                    <td className="px-6 py-4">
                                        <div className="flex items-center gap-2">
                                            {tx.type === "DEPOSIT" ? (
                                                <ArrowUpCircle size={14} className="text-green-500" />
                                            ) : (
                                                <ArrowDownCircle size={14} className="text-red-500" />
                                            )}
                                            <span className="text-[10px] font-bold text-gray-700 uppercase tracking-tight">{tx.type}</span>
                                        </div>
                                    </td>
                                    <td className="px-6 py-4">
                                        <p className={`text-sm font-bold tracking-tighter ${tx.amount > 0 ? 'text-green-600' : 'text-red-600'}`}>
                                            {tx.amount > 0 ? '+' : ''}₺{tx.amount}
                                        </p>
                                    </td>
                                    <td className="px-6 py-4">
                                        <span className={`px-2 py-0.5 rounded text-[9px] font-bold uppercase ${tx.status === 'completed' ? 'bg-green-50 text-green-600' : 'bg-orange-50 text-orange-600'}`}>
                                            {tx.status}
                                        </span>
                                    </td>
                                    <td className="px-6 py-4 text-right">
                                        <div className="flex flex-col items-end">
                                            <p className="text-[10px] font-bold text-gray-500">{tx.createdAt?.toDate()?.toLocaleDateString('tr-TR')}</p>
                                            <p className="text-[9px] font-medium text-gray-400">{tx.createdAt?.toDate()?.toLocaleTimeString('tr-TR', { hour: '2-digit', minute: '2-digit' })}</p>
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

function SummaryCard({ title, value, icon: Icon, color }: any) {
    const colors: any = {
        blue: "text-blue-600 bg-blue-50 border-blue-100",
        green: "text-green-600 bg-green-50 border-green-100",
        indigo: "text-indigo-600 bg-indigo-50 border-indigo-100",
    };

    return (
        <div className="mica-panel p-6 rounded-3xl bg-white border border-gray-100 shadow-sm flex items-center gap-4">
            <div className={`w-12 h-12 rounded-2xl flex items-center justify-center shrink-0 border ${colors[color]}`}>
                <Icon size={24} />
            </div>
            <div>
                <p className="text-[10px] font-bold text-gray-400 uppercase tracking-widest">{title}</p>
                <h4 className="text-2xl font-bold text-gray-900 tracking-tight">{value}</h4>
            </div>
        </div>
    );
}
