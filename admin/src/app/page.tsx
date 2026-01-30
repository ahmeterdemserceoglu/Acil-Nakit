"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import {
  TrendingUp,
  Users,
  Wallet,
  AlertCircle,
  AlertTriangle,
  ArrowUpRight,
  ArrowDownRight,
  ChevronRight,
  Activity,
  ShieldAlert,
  ShieldCheck,
  MessageSquare
} from "lucide-react";
import { useRouter } from "next/navigation";
import { auth, db } from "@/lib/firebase";
import { collection, query, where, getDocs, limit, orderBy, doc, getDoc } from "firebase/firestore";
import { motion, AnimatePresence } from "framer-motion";

const containerVariants = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: {
      staggerChildren: 0.05
    }
  }
} as const;

const itemVariants = {
  hidden: { y: 20, opacity: 0 },
  visible: {
    y: 0,
    opacity: 1,
    transition: {
      type: "spring",
      stiffness: 100,
      damping: 15
    }
  }
} as const;

export default function Dashboard() {
  const [stats, setStats] = useState({
    totalUsers: 0,
    activeTasks: 0,
    totalRevenue: 0,
    activeEscrow: 0,
    pendingWithdrawals: 0,
    pendingVerifications: 0,
    pendingDisputes: 0,
    riskyChats: 0,
    boostedTasks: 0
  });
  const [campusActivity, setCampusActivity] = useState<any[]>([]);
  const [securityAlerts, setSecurityAlerts] = useState<any[]>([]);
  const [recentTransactions, setRecentTransactions] = useState<any[]>([]);
  const [adminData, setAdminData] = useState({ name: "Yükleniyor...", role: "Admin" });
  const [loading, setLoading] = useState(true);

  const router = useRouter();

  const handleRefresh = () => {
    fetchData();
  };

  async function fetchData() {
    setLoading(true);
    try {
      const usersSnap = await getDocs(collection(db, "users"));
      const withdrawalsSnap = await getDocs(query(collection(db, "withdrawals"), where("status", "==", "pending")));
      const completedWithdrawalsSnap = await getDocs(query(collection(db, "withdrawals"), where("status", "==", "completed")));
      const verifiedSnap = await getDocs(query(collection(db, "users"), where("isVerified", "==", false)));
      const tasksSnap = await getDocs(query(collection(db, "tasks"), where("status", "in", ["OPEN", "IN_PROGRESS"])));
      const disputesSnap = await getDocs(query(collection(db, "disputes"), where("status", "==", "PENDING")));
      const alertsSnap = await getDocs(query(collection(db, "security_alerts"), where("status", "==", "PENDING"), limit(5)));
      const boostedSnap = await getDocs(query(collection(db, "tasks"), where("isBoosted", "==", true), where("status", "in", ["OPEN", "IN_PROGRESS"])));

      const depositsSnap = await getDocs(query(
        collection(db, "transactions"),
        where("type", "==", "DEPOSIT"),
        where("status", "==", "completed")
      ));

      let totalDepositRevenue = 0;
      depositsSnap.forEach(doc => {
        // %8 Komisyon
        totalDepositRevenue += (doc.data().amount || 0) * 0.08;
      });

      let totalWithdrawalRevenue = 0;
      completedWithdrawalsSnap.forEach(doc => {
        // Çekim ücretleri (sabit 10 TL veya %2 varsayalım, ama veride ne varsa)
        totalWithdrawalRevenue += doc.data().fee || 0;
      });

      // Escrow Toplamı
      let totalEscrow = 0;
      usersSnap.forEach(doc => {
        totalEscrow += doc.data().escrowBalance || 0;
      });

      // Heatmap Processing
      const campusMap: any = {};
      tasksSnap.forEach(tDoc => {
        const campus = tDoc.data().campusName || "Genel";
        campusMap[campus] = (campusMap[campus] || 0) + 1;
      });

      const heatmapData = Object.entries(campusMap)
        .map(([name, count]: any) => ({ name, count }))
        .sort((a, b) => b.count - a.count);

      setCampusActivity(heatmapData);
      setSecurityAlerts(alertsSnap.docs.map(d => ({ id: d.id, ...d.data() })));

      const recentTxSnap = await getDocs(query(
        collection(db, "transactions"),
        orderBy("createdAt", "desc"),
        limit(6)
      ));

      const usersList = usersSnap.docs.map(d => ({ id: d.id, ...d.data() }));

      const txList = recentTxSnap.docs.map(tDoc => {
        const txData = tDoc.data();
        const userObj: any = usersList.find((u: any) => u.id === txData.userId);
        return {
          id: tDoc.id,
          ...txData,
          userName: userObj?.name || "Bilinmeyen Kullanıcı",
          dateStr: txData.createdAt?.toDate() ? txData.createdAt.toDate().toLocaleString('tr-TR', { hour: '2-digit', minute: '2-digit' }) : "Yeni"
        };
      });

      setRecentTransactions(txList);
      setStats({
        totalUsers: usersSnap.size,
        activeTasks: tasksSnap.size,
        totalRevenue: totalDepositRevenue + totalWithdrawalRevenue,
        activeEscrow: totalEscrow,
        pendingWithdrawals: withdrawalsSnap.size,
        pendingVerifications: verifiedSnap.size,
        pendingDisputes: disputesSnap.size,
        riskyChats: alertsSnap.size,
        boostedTasks: boostedSnap.size
      });
    } catch (e) {
      console.error("Dashboard data error:", e);
    }
    setLoading(false);
  }

  useEffect(() => {
    const fetchAdminInfo = async () => {
      const user = auth.currentUser;
      if (user) {
        const uDoc = await getDoc(doc(db, "users", user.uid));
        if (uDoc.exists()) {
          setAdminData({
            name: uDoc.data().name || "Admin",
            role: "Sistem Mimarı"
          });
        }
      }
    };
    fetchAdminInfo();
    fetchData();
  }, []);

  return (
    <motion.div
      initial="hidden"
      animate="visible"
      variants={containerVariants}
      className="max-w-[1400px] mx-auto space-y-8 py-6 px-4"
    >
      <header className="flex items-center justify-between mb-2">
        <div>
          <h2 className="text-3xl font-bold text-gray-900 tracking-tight">Kontrol Merkezi</h2>
          <p className="text-sm font-medium text-gray-400 mt-1">Sistem durumu ve finansal özet.</p>
        </div>
        <div className="flex items-center gap-4">
          <button
            onClick={handleRefresh}
            className="flex items-center gap-2 bg-white hover:bg-gray-50 text-gray-600 px-5 py-2.5 rounded-xl border border-gray-200 shadow-sm transition-all text-xs font-semibold uppercase tracking-wider active:scale-95"
          >
            <Activity size={14} className={loading ? "animate-spin" : "text-blue-500"} />
            Verileri Yenile
          </button>
          <div className="h-10 w-[1px] bg-gray-200 mx-2 hidden sm:block" />
          <div className="flex items-center gap-3">
            <div className="text-right hidden sm:block">
              <p className="text-xs font-bold text-gray-900 leading-none">{adminData.name}</p>
              <p className="text-[10px] font-semibold text-blue-500 uppercase tracking-widest mt-1">{adminData.role}</p>
            </div>
            <div className="w-10 h-10 rounded-xl bg-gray-900 flex items-center justify-center text-white font-bold text-sm shadow-lg">
              {adminData.name[0]}
            </div>
          </div>
        </div>
      </header>

      {/* Primary KPIs */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
        {[
          { href: "/users", title: "Toplam Kullanıcı", value: stats.totalUsers.toLocaleString(), icon: Users, color: "blue" },
          { title: "Net Tahmini Gelir", value: `₺${stats.totalRevenue.toLocaleString(undefined, { minimumFractionDigits: 0 })}`, icon: TrendingUp, color: "green" },
          { title: "Aktif Escrow", value: `₺${stats.activeEscrow.toLocaleString(undefined, { minimumFractionDigits: 0 })}`, icon: ShieldCheck, color: "indigo" },
          { title: "Flash Task Karı", value: `₺${(stats.boostedTasks * 10).toLocaleString()}`, icon: TrendingUp, color: "purple" }
        ].map((card, i) => (
          <motion.div key={i} variants={itemVariants}>
            <StatCard {...card} />
          </motion.div>
        ))}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        {/* Left Column: Alerts & Monitor */}
        <div className="lg:col-span-4 space-y-6">
          <motion.div variants={itemVariants} className="mica-panel p-6 rounded-3xl border border-gray-100 shadow-sm bg-white">
            <h3 className="text-sm font-bold text-gray-900 mb-6 flex items-center gap-2 uppercase tracking-wider">
              <AlertCircle size={16} className="text-blue-500" />
              Bekleyen İşlemler
            </h3>
            <div className="grid grid-cols-2 gap-4">
              <AlertItem href="/withdrawals" label="Para Çekme" count={stats.pendingWithdrawals} color="orange" />
              <AlertItem href="/disputes" label="Uyuşmazlık" count={stats.pendingDisputes} color="red" />
              <AlertItem href="/verification" label="Doğrulama" count={stats.pendingVerifications} color="blue" />
              <AlertItem label="Riskli Chat" count={stats.riskyChats} color="red" />
            </div>
          </motion.div>

          {/* Security AI Feed */}
          <motion.div variants={itemVariants} className="mica-panel p-6 rounded-3xl border border-gray-100 shadow-sm bg-gray-50/30">
            <div className="flex items-center justify-between mb-6">
              <h3 className="text-sm font-bold text-gray-900 flex items-center gap-2 uppercase tracking-wider">
                <ShieldAlert size={16} className="text-red-500" />
                AI Güvenlik Duvarı
              </h3>
              {stats.riskyChats > 0 && (
                <span className="flex h-2 w-2 rounded-full bg-red-500 animate-ping" />
              )}
            </div>
            <div className="space-y-3">
              {securityAlerts.length === 0 ? (
                <p className="text-xs font-medium text-gray-400 text-center py-6 italic">Tehdit algılanmadı.</p>
              ) : securityAlerts.map(alert => (
                <div key={alert.id} className="p-3 bg-white rounded-xl border border-gray-100 shadow-sm hover:border-red-100 transition-all">
                  <p className="text-[11px] font-semibold text-gray-800 line-clamp-1 italic">"{alert.content}"</p>
                  <div className="mt-1 flex items-center justify-between">
                    <span className="text-[9px] font-bold text-red-500 uppercase">İhlal</span>
                    <span className="text-[9px] font-medium text-gray-400 italic">ID: {alert.chatId.substring(0, 8)}</span>
                  </div>
                </div>
              ))}
            </div>
          </motion.div>
        </div>

        {/* Right Column: Radar & Transactions */}
        <div className="lg:col-span-8 space-y-6">
          <motion.div variants={itemVariants} className="mica-panel p-8 rounded-3xl border border-gray-100 shadow-sm bg-white relative overflow-hidden">
            <div className="flex items-center justify-between mb-8">
              <div>
                <h3 className="text-lg font-bold text-gray-900 flex items-center gap-2">
                  <Activity size={20} className="text-blue-600" />
                  Kampüs Yoğunluk Radarı
                </h3>
                <p className="text-xs font-medium text-gray-400 mt-1 uppercase tracking-wider">Canlı Aktivite Isı Haritası</p>
              </div>
              <Link href="/tasks" className="text-xs font-bold text-blue-600 hover:underline">Tümünü Gör</Link>
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
              {campusActivity.map((campus, idx) => (
                <div key={campus.name} className="p-4 rounded-2xl bg-gray-50/50 border border-gray-100">
                  <div className="flex items-end justify-between mb-2">
                    <span className="text-2xl font-bold text-gray-900">{campus.count}</span>
                    <span className="text-[10px] font-bold text-blue-500 uppercase">Aktif</span>
                  </div>
                  <p className="text-[10px] font-bold text-gray-500 uppercase truncate">{campus.name}</p>
                </div>
              ))}
            </div>
          </motion.div>

          <motion.div variants={itemVariants} className="mica-panel p-8 rounded-3xl border border-gray-100 shadow-sm bg-white">
            <div className="flex items-center justify-between mb-8">
              <h3 className="text-lg font-bold text-gray-900 flex items-center gap-2 uppercase tracking-tight">
                <Wallet size={20} className="text-gray-400" />
                Son Hareketler
              </h3>
              <Link href="/transactions" className="text-xs font-bold text-gray-400 hover:text-gray-900 transition-colors uppercase tracking-widest">Tüm Arşiv</Link>
            </div>
            <div className="space-y-1">
              {recentTransactions.map((tx: any) => (
                <TransactionItem
                  key={tx.id}
                  name={tx.userName}
                  type={tx.type}
                  amount={tx.amount.toLocaleString()}
                  date={tx.dateStr}
                />
              ))}
            </div>
          </motion.div>
        </div>
      </div>
    </motion.div>
  );
}

function StatCard({ title, value, icon: Icon, color, href }: any) {
  const colors: any = {
    blue: "text-blue-600 bg-blue-50",
    green: "text-green-600 bg-green-50",
    indigo: "text-indigo-600 bg-indigo-50",
    purple: "text-purple-600 bg-purple-50",
  };

  const Card = (
    <div className="mica-panel p-6 rounded-3xl bg-white border border-gray-100 hover:shadow-lg transition-all duration-300 group">
      <div className={`w-10 h-10 rounded-xl flex items-center justify-center mb-4 ${colors[color]}`}>
        <Icon size={20} />
      </div>
      <p className="text-[10px] font-bold text-gray-400 uppercase tracking-widest">{title}</p>
      <h4 className="text-2xl font-bold mt-1 text-gray-900 tracking-tight">{value}</h4>
    </div>
  );

  return href ? <Link href={href}>{Card}</Link> : Card;
}

function AlertItem({ label, count, color, href }: any) {
  const variants: any = {
    orange: "text-orange-600 bg-orange-50 border-orange-100",
    red: "text-red-600 bg-red-50 border-red-100",
    blue: "text-blue-600 bg-blue-50 border-blue-100",
  };

  const Content = (
    <div className={`p-4 rounded-2xl border ${variants[color]} flex flex-col items-center justify-center text-center transition-transform hover:scale-[1.02] cursor-pointer`}>
      <span className="text-lg font-bold">{count}</span>
      <span className="text-[9px] font-bold uppercase tracking-wider mt-1 opacity-80">{label}</span>
    </div>
  );

  return href ? <Link href={href} className="contents">{Content}</Link> : Content;
}

function TransactionItem({ name, type, amount, date }: any) {
  const isDeposit = type === 'DEPOSIT';
  return (
    <div className="flex items-center justify-between p-3 hover:bg-gray-50 rounded-xl transition-all group">
      <div className="flex items-center gap-3">
        <div className={`w-8 h-8 rounded-lg flex items-center justify-center font-bold text-[10px] ${isDeposit ? 'bg-green-50 text-green-600' : 'bg-red-50 text-red-600'}`}>
          {name[0]}
        </div>
        <div>
          <p className="text-xs font-bold text-gray-800">{name}</p>
          <p className="text-[9px] font-medium text-gray-400 uppercase">{type}</p>
        </div>
      </div>
      <div className="text-right">
        <p className={`text-xs font-bold ${isDeposit ? 'text-green-600' : 'text-red-600'}`}>
          {isDeposit ? '+' : '-'} ₺{amount}
        </p>
        <p className="text-[9px] font-medium text-gray-400">{date}</p>
      </div>
    </div>
  );
}
