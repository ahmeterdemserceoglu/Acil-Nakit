"use client";

import Link from "next/link";
import { useState, useEffect } from "react";
import { usePathname, useRouter } from "next/navigation";
import { signOut } from "firebase/auth";
import { auth } from "@/lib/firebase";
import {
    LayoutDashboard,
    UserCheck,
    Users,
    Wallet,
    AlertTriangle,
    Settings,
    LogOut,
    ChevronRight,
    ListTodo,
    ReceiptText,
    User,
    Clock
} from "lucide-react";
import { motion } from "framer-motion";
import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

function cn(...inputs: ClassValue[]) {
    return twMerge(clsx(inputs));
}

const menuItems = [
    { id: "dashboard", label: "Dashboard", icon: LayoutDashboard, href: "/" },
    { id: "users", label: "Kullanıcılar", icon: Users, href: "/users" },
    { id: "tasks", label: "Görevler", icon: ListTodo, href: "/tasks" },
    { id: "verification", label: "Doğrulamalar", icon: UserCheck, href: "/verification" },
    { id: "withdrawals", label: "Para Çekme", icon: Wallet, href: "/withdrawals" },
    { id: "transactions", label: "İşlemler", icon: ReceiptText, href: "/transactions" },
    { id: "disputes", label: "Şikayetler", icon: AlertTriangle, href: "/disputes" },
    { id: "profile", label: "Profil", icon: User, href: "/profile" },
];

export default function Sidebar() {
    const pathname = usePathname();
    const router = useRouter();

    const handleLogout = async () => {
        try {
            await signOut(auth);
            router.push("/login");
        } catch (error) {
            console.error("Logout error:", error);
        }
    };

    const [timeLeft, setTimeLeft] = useState<string | null>(null);

    useEffect(() => {
        const timer = setInterval(() => {
            const lastVerified = sessionStorage.getItem(`2fa_verified_at_${auth.currentUser?.uid}`);
            if (lastVerified) {
                const now = Date.now();
                const sessionDuration = 15 * 60 * 1000;
                const diff = (parseInt(lastVerified) + sessionDuration) - now;

                if (diff <= 0) {
                    setTimeLeft("Süre Doldu");
                    clearInterval(timer);
                    window.location.reload();
                } else {
                    const mins = Math.floor(diff / 60000);
                    const secs = Math.floor((diff % 60000) / 1000);
                    setTimeLeft(`${mins}:${secs < 10 ? '0' : ''}${secs}`);
                }
            } else {
                setTimeLeft(null);
            }
        }, 1000);

        return () => clearInterval(timer);
    }, []);

    return (
        <aside className="w-64 bg-white border-r border-gray-100 h-screen fixed left-0 top-0 z-50 flex flex-col overflow-hidden">
            {/* Logo Section - Fixed */}
            <div className="p-8 pb-4">
                <div className="flex items-center gap-3">
                    <div className="w-10 h-10 bg-blue-600 rounded-xl flex items-center justify-center shadow-lg shadow-blue-200">
                        <LayoutDashboard className="text-white" size={24} />
                    </div>
                    <div>
                        <h1 className="text-xl font-bold text-gray-900 tracking-tighter">AcilNakit</h1>
                        <p className="text-[10px] font-bold text-blue-600 uppercase tracking-widest">Admin Panel</p>
                    </div>
                </div>
            </div>

            {/* Navigation Section - Scrollable */}
            <div className="flex-1 overflow-y-auto px-8 py-4 custom-scrollbar">
                <nav className="space-y-1.5 font-sans">
                    {menuItems.map((item) => {
                        const Icon = item.icon;
                        const isActive = pathname === item.href;

                        return (
                            <Link
                                key={item.id}
                                href={item.href}
                                className={cn(
                                    "flex items-center justify-between px-4 py-3 rounded-2xl text-sm font-bold transition-all duration-300 group",
                                    isActive
                                        ? "bg-blue-50 text-blue-600 shadow-sm"
                                        : "text-gray-400 hover:bg-gray-50 hover:text-gray-900"
                                )}
                            >
                                <div className="flex items-center gap-3">
                                    <Icon size={20} className={cn(
                                        "transition-transform duration-300 group-hover:scale-110",
                                        isActive ? "text-blue-600" : "text-gray-400 group-hover:text-gray-900"
                                    )} />
                                    <span className="tracking-tight">{item.label}</span>
                                </div>
                                {isActive && (
                                    <motion.div
                                        layoutId="active-pill"
                                        className="w-1.5 h-1.5 bg-blue-600 rounded-full"
                                    />
                                )}
                            </Link>
                        );
                    })}
                </nav>
            </div>

            {/* Bottom Section - Fixed at the bottom */}
            <div className="p-6 space-y-4 border-t border-gray-50 bg-white/50 backdrop-blur-sm">
                {timeLeft && (
                    <div className="mica-panel p-4 rounded-2xl bg-gray-50 border border-gray-100 relative overflow-hidden group">
                        <div className="flex items-center justify-between mb-2">
                            <span className="text-[10px] font-bold text-gray-400 uppercase tracking-widest text-blue-600/70">Oturum Süresi</span>
                            <Clock size={14} className="text-blue-500 animate-pulse" />
                        </div>
                        <div className="flex items-baseline gap-1">
                            <span className="text-2xl font-black text-gray-900 tracking-tighter">{timeLeft}</span>
                            <span className="text-[10px] font-bold text-gray-400">KALDI</span>
                        </div>
                        {/* Progress bar simulation */}
                        <div className="mt-3 h-1 w-full bg-gray-200 rounded-full overflow-hidden">
                            <motion.div
                                className="h-full bg-blue-600"
                                initial={{ width: "100%" }}
                                animate={{ width: "0%" }}
                                transition={{ duration: 15 * 60, ease: "linear" }}
                            />
                        </div>
                    </div>
                )}

                <button
                    onClick={handleLogout}
                    className="w-full flex items-center gap-3 px-4 py-3 text-red-500 hover:bg-red-50 rounded-2xl text-sm font-bold transition-all duration-300 group"
                >
                    <LogOut size={20} className="group-hover:-translate-x-1 transition-transform" />
                    <span>Çıkış Yap</span>
                </button>
            </div>
        </aside>
    );
}
