"use client";

import { useState, useEffect } from "react";
import { auth, functions, db } from "@/lib/firebase";
import { signInWithPopup, GoogleAuthProvider, signOut } from "firebase/auth";
import { httpsCallable } from "firebase/functions";
import { doc, getDoc } from "firebase/firestore";
import { Lock, ShieldCheck, Zap, TrendingUp, KeyRound, ArrowRight, Mail } from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";

export default function LoginPage({ is2FAForced = false }: { is2FAForced?: boolean }) {
    const [loading, setLoading] = useState(false);
    const [step, setStep] = useState<'google' | '2fa'>(is2FAForced ? '2fa' : 'google');
    const [code, setCode] = useState("");
    const [error, setError] = useState("");

    useEffect(() => {
        if (is2FAForced && step !== '2fa') {
            setStep('2fa');
            sendCode();
        }
    }, [is2FAForced]);

    const sendCode = async () => {
        setLoading(true);
        try {
            const send2FA = httpsCallable(functions, 'sendAdmin2FACode');
            await send2FA();
        } catch (e: any) {
            setError("Kod gönderilemedi: " + e.message);
        }
        setLoading(false);
    };

    const handleGoogleLogin = async () => {
        setLoading(true);
        setError("");
        try {
            const provider = new GoogleAuthProvider();
            const result = await signInWithPopup(auth, provider);

            // 2FA kontrolü
            const userDoc = await getDoc(doc(db, "users", result.user.uid));
            if (userDoc.exists() && userDoc.data().isAdmin) {
                if (userDoc.data().twoFactorEnabled) {
                    setStep('2fa');
                    await sendCode();
                } else {
                    window.location.reload();
                }
            } else {
                // Admin değilse RootLayout hata mesajı gösterecek zaten ama biz yine de logout yapalım temiz kalsın
                // Aslında RootLayout LoginPage'i renders ediyor isAdmin false ise.
                window.location.reload();
            }
        } catch (e: any) {
            console.error(e);
            setError("Giriş başarısız.");
        }
        setLoading(false);
    };

    const handleVerify2FA = async () => {
        if (code.length !== 6) return;
        setLoading(true);
        setError("");
        try {
            const verify2FA = httpsCallable(functions, 'verifyAdmin2FACode');
            await verify2FA({ code });

            // Başarılı!
            const now = Date.now().toString();
            sessionStorage.setItem(`2fa_verified_${auth.currentUser?.uid}`, 'true');
            sessionStorage.setItem(`2fa_verified_at_${auth.currentUser?.uid}`, now);
            window.location.reload();
        } catch (e: any) {
            setError("Hatalı veya süresi dolmuş kod.");
        }
        setLoading(false);
    };

    const handleLogout = async () => {
        await signOut(auth);
        setStep('google');
        window.location.reload();
    };

    return (
        <div className="min-h-screen flex items-center justify-center bg-[#f0f2f5] overflow-hidden relative font-sans">
            {/* Background Decorative Elements */}
            <div className="absolute top-[-10%] left-[-10%] w-[40%] h-[40%] bg-[var(--isbank-blue)] opacity-[0.03] rounded-full blur-[120px]" />
            <div className="absolute bottom-[-10%] right-[-10%] w-[40%] h-[40%] bg-blue-600 opacity-[0.03] rounded-full blur-[120px]" />

            <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                className="relative z-10 w-full max-w-[1000px] flex flex-col md:flex-row bg-white/70 backdrop-blur-2xl rounded-[40px] overflow-hidden shadow-2xl border border-white/40 min-h-[600px]"
            >
                {/* Left Side: Branding */}
                <div className="flex-1 bg-gradient-to-br from-[#0038A8] to-[#0054FF] p-12 text-white flex flex-col justify-between relative overflow-hidden">
                    <div className="absolute top-0 right-0 w-full h-full opacity-10 pointer-events-none">
                        <svg className="w-full h-full" viewBox="0 0 100 100" preserveAspectRatio="none">
                            <path d="M0,0 L100,0 L100,100 L0,100 Z" fill="url(#grad)" />
                            <defs>
                                <linearGradient id="grad" x1="0%" y1="0%" x2="100%" y2="100%">
                                    <stop offset="0%" style={{ stopColor: 'white', stopOpacity: 0.2 }} />
                                    <stop offset="100%" style={{ stopColor: 'transparent', stopOpacity: 0 }} />
                                </linearGradient>
                            </defs>
                        </svg>
                    </div>

                    <div className="z-20">
                        <div className="w-16 h-16 bg-white/20 backdrop-blur-md rounded-2xl flex items-center justify-center mb-8">
                            <ShieldCheck size={32} />
                        </div>
                        <h1 className="text-5xl font-black tracking-tighter italic mb-4 leading-none">
                            ACILNAKIT <br />
                            <span className="text-blue-200">ADMIN</span>
                        </h1>
                        <p className="text-blue-100 text-lg font-medium max-w-xs opacity-80 leading-relaxed">
                            Kampüs ekonomisinin kontrol kulesine hoş geldin. Her şey senin gözetiminde.
                        </p>
                    </div>

                    <div className="z-20 space-y-6 pt-12 border-t border-white/10">
                        <div className="flex items-center space-x-4">
                            <div className="w-10 h-10 rounded-xl bg-white/10 flex items-center justify-center"><Zap size={20} /></div>
                            <p className="text-sm font-semibold text-blue-50 tracking-tight">Hızlı onay, anında para transferi.</p>
                        </div>
                    </div>
                </div>

                {/* Right Side: Step Handling */}
                <div className="w-full md:w-[450px] bg-white/90 p-12 flex flex-col justify-center">
                    <AnimatePresence mode="wait">
                        {step === 'google' ? (
                            <motion.div
                                key="google"
                                initial={{ opacity: 0, x: 20 }}
                                animate={{ opacity: 1, x: 0 }}
                                exit={{ opacity: 0, x: -20 }}
                                className="space-y-8"
                            >
                                <div>
                                    <h2 className="text-3xl font-bold text-gray-900 tracking-tight">Giriş Yap</h2>
                                    <p className="text-gray-500 font-medium text-sm mt-2">Sadece yetkili yöneticiler içindir.</p>
                                </div>

                                <div className="space-y-4">
                                    <button
                                        onClick={handleGoogleLogin}
                                        disabled={loading}
                                        className="group w-full py-4 px-6 bg-[#1a1a1a] text-white rounded-2xl font-bold flex items-center justify-center space-x-4 hover:bg-black transition-all active:scale-95 shadow-xl shadow-black/10 relative overflow-hidden"
                                    >
                                        {loading ? <div className="w-6 h-6 border-4 border-white border-t-transparent rounded-full animate-spin" /> :
                                            <>
                                                <img src="https://www.google.com/favicon.ico" className="w-5 h-5" alt="G" />
                                                <span>GOOGLE İLE OTURUM AÇ</span>
                                            </>}
                                    </button>

                                    {error && <p className="text-red-500 text-xs font-bold text-center">{error}</p>}
                                </div>
                            </motion.div>
                        ) : (
                            <motion.div
                                key="2fa"
                                initial={{ opacity: 0, x: 20 }}
                                animate={{ opacity: 1, x: 0 }}
                                exit={{ opacity: 0, x: -20 }}
                                className="space-y-8"
                            >
                                <div>
                                    <div className="w-12 h-12 bg-blue-50 text-blue-600 rounded-xl flex items-center justify-center mb-6">
                                        <KeyRound size={24} />
                                    </div>
                                    <h2 className="text-3xl font-bold text-gray-900 tracking-tight">İki Faktörlü Doğrulama</h2>
                                    <p className="text-gray-500 font-medium text-sm mt-2">
                                        E-posta adresinize gönderilen 6 haneli kodu girin.
                                    </p>
                                </div>

                                <div className="space-y-6">
                                    <div className="space-y-2">
                                        <div className="relative">
                                            <input
                                                type="text"
                                                maxLength={6}
                                                placeholder="000000"
                                                value={code}
                                                onChange={(e) => setCode(e.target.value.replace(/\D/g, ''))}
                                                className="w-full py-4 px-6 bg-gray-50 border-2 border-gray-100 rounded-2xl text-2xl font-bold tracking-[1rem] text-center focus:border-blue-500 focus:bg-white transition-all outline-none"
                                            />
                                            <Mail className="absolute right-6 top-1/2 -translate-y-1/2 text-gray-300" size={20} />
                                        </div>
                                        {error && <p className="text-red-500 text-xs font-bold text-center">{error}</p>}
                                    </div>

                                    <button
                                        onClick={handleVerify2FA}
                                        disabled={loading || code.length !== 6}
                                        className="w-full py-4 px-6 bg-blue-600 text-white rounded-2xl font-bold flex items-center justify-center space-x-3 hover:bg-blue-700 transition-all disabled:opacity-50 disabled:cursor-not-allowed group shadow-lg shadow-blue-200"
                                    >
                                        {loading ? <div className="w-6 h-6 border-4 border-white border-t-transparent rounded-full animate-spin" /> :
                                            <>
                                                <span>DOĞRULAYI VE GİRİŞ YAP</span>
                                                <ArrowRight size={20} className="group-hover:translate-x-1 transition-transform" />
                                            </>}
                                    </button>

                                    <div className="flex flex-col gap-3">
                                        <button
                                            onClick={sendCode}
                                            disabled={loading}
                                            className="text-[10px] font-bold text-blue-600 uppercase tracking-widest hover:underline"
                                        >
                                            Yeni Kod Gönder
                                        </button>
                                        <button
                                            onClick={handleLogout}
                                            className="text-[10px] font-bold text-gray-400 uppercase tracking-widest hover:text-red-500 transition-colors"
                                        >
                                            Farklı Hesapla Giriş Yap
                                        </button>
                                    </div>
                                </div>
                            </motion.div>
                        )}
                    </AnimatePresence>
                </div>
            </motion.div>
        </div>
    );
}
