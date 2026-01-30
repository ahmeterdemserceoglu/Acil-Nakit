import { initializeApp, getApps } from "firebase/app";
import { getFirestore } from "firebase/firestore";
import { getAuth } from "firebase/auth";
import { getStorage } from "firebase/storage";
import { getFunctions } from "firebase/functions";
const firebaseConfig = {
    apiKey: "AIzaSyCRJYwLXFfiun3VweNiAESGBhDzsvsB3UE",
    authDomain: "gane-35146.firebaseapp.com",
    databaseURL: "https://gane-35146-default-rtdb.firebaseio.com",
    projectId: "gane-35146",
    storageBucket: "gane-35146.firebasestorage.app",
    messagingSenderId: "777166045750",
    appId: "1:777166045750:web:7f1afa04705495824d0e97",
    measurementId: "G-PBQ7ZQC916"
};
const app = getApps().length === 0 ? initializeApp(firebaseConfig) : getApps()[0];
export const db = getFirestore(app);
export const auth = getAuth(app);
export const storage = getStorage(app);
export const functions = getFunctions(app);
