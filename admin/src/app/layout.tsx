"use client";

import { Inter } from "next/font/google";
import "./globals.css";
import { useEffect, useState } from "react";
import { onAuthStateChanged, User } from "firebase/auth";
import { auth, db } from "@/lib/firebase";
import { doc, getDoc } from "firebase/firestore";
import LoginPage from "./login/page";
import Sidebar from "@/components/Sidebar";
import { AlertTriangle } from "lucide-react";

const inter = Inter({ subsets: ["latin"] });

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const [user, setUser] = useState<User | null>(null);
  const [isAdmin, setIsAdmin] = useState<boolean | null>(null);
  const [needs2FA, setNeeds2FA] = useState<boolean>(false);
  const [ipError, setIpError] = useState<boolean>(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, async (currentUser) => {
      setLoading(true);
      setIpError(false);

      if (currentUser) {
        setUser(currentUser);
        // Admin kontrolü
        const userDoc = await getDoc(doc(db, "users", currentUser.uid));
        if (userDoc.exists() && userDoc.data().isAdmin === true) {
          const userData = userDoc.data();
          setIsAdmin(true);

          // 1. IP Kısıtlaması Kontrolü
          if (userData.ipRestriction === true) {
            try {
              const ipRes = await fetch('https://api.ipify.org?format=json');
              const { ip } = await ipRes.json();

              if (userData.authorizedIp && userData.authorizedIp !== ip) {
                setIpError(true);
                setIsAdmin(false); // Yetkiyi düşür
                setLoading(false);
                return;
              }
            } catch (e) {
              console.error("IP check failed:", e);
              // Güvenlik gereği IP kontrolü başarısızsa eriştirme
              setIpError(true);
              setIsAdmin(false);
              setLoading(false);
              return;
            }
          }

          // 2. 2FA Kontrolü (15 dk kod süresi, 15 dk aktif oturum süresi)
          if (userData.twoFactorEnabled === true) {
            const lastVerified = sessionStorage.getItem(`2fa_verified_at_${currentUser.uid}`);
            const now = Date.now();
            const sessionDuration = 15 * 60 * 1000; // 15 Dakika

            if (!lastVerified || (now - parseInt(lastVerified)) > sessionDuration) {
              setNeeds2FA(true);
              // Süre dolduysa session'ı temizle
              sessionStorage.removeItem(`2fa_verified_${currentUser.uid}`);
              sessionStorage.removeItem(`2fa_verified_at_${currentUser.uid}`);
            } else {
              setNeeds2FA(false);
            }
          } else {
            setNeeds2FA(false);
          }
        } else {
          setIsAdmin(false);
          setNeeds2FA(false);
        }
      } else {
        setUser(null);
        setIsAdmin(null);
        setNeeds2FA(false);
      }
      setLoading(false);
    });

    return () => unsubscribe();
  }, []);

  if (loading) {
    return (
      <html lang="tr">
        <body className="flex items-center justify-center min-h-screen bg-white">
          <div className="w-10 h-10 border-4 border-blue-600 border-t-transparent rounded-full animate-spin"></div>
        </body>
      </html>
    );
  }

  if (ipError) {
    return (
      <html lang="tr">
        <body className="bg-white">
          <div className="flex flex-col items-center justify-center min-h-screen p-6 text-center">
            <div className="w-20 h-20 bg-red-50 text-red-600 rounded-3xl flex items-center justify-center mb-6">
              <AlertTriangle size={40} />
            </div>
            <h1 className="text-2xl font-black text-gray-900 tracking-tighter sm:text-3xl">ERİŞİM ENGELLENDİ</h1>
            <p className="text-gray-500 mt-2 max-w-md font-medium">Bulunduğunuz IP adresi bu yönetim paneline erişim yetkisine sahip değil. Güvenlik protokolü gereği girişiniz engellendi.</p>
            <button onClick={() => window.location.reload()} className="mt-8 px-8 py-3 bg-gray-900 text-white rounded-2xl font-bold text-sm hover:bg-gray-800 transition-all">TEKRAR DENE</button>
          </div>
        </body>
      </html>
    );
  }

  if (!user || isAdmin === false || needs2FA) {
    return (
      <html lang="tr">
        <body>
          <LoginPage is2FAForced={needs2FA} />
          {!isAdmin && user && !needs2FA && (
            <div className="fixed bottom-10 left-1/2 -translate-x-1/2 bg-red-600 text-white px-6 py-3 rounded-full font-bold shadow-2xl">
              Yetkisiz Erişim: Bu hesap yönetici değil.
            </div>
          )}
        </body>
      </html>
    );
  }

  return (
    <html lang="tr">
      <body className="bg-[#f8fafc]">
        <div className="flex min-h-screen">
          <Sidebar />
          <main className="flex-1 ml-64 p-8">
            <div className="max-w-7xl mx-auto">
              {children}
            </div>
          </main>
        </div>
      </body>
    </html>
  );
}
