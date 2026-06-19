import {StrictMode} from 'react'
import {createRoot} from 'react-dom/client'
import {BrowserRouter, Routes, Route, Navigate} from "react-router";
import './index.css'
import AdminLayout from './admin/AdminLayout.jsx'
import User from './admin/User.jsx'

createRoot(document.getElementById('root')).render(
    <StrictMode>
        <BrowserRouter>
            <Routes>
                <Route path="/admin" element={<AdminLayout/>}>
                    <Route index element={<Navigate to="user" replace/>}/>
                    <Route path="user" element={<User/>}/>
                </Route>
            </Routes>
        </BrowserRouter>
    </StrictMode>,
)
