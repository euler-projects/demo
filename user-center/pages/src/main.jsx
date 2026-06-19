import {StrictMode} from 'react'
import {createRoot} from 'react-dom/client'
import {BrowserRouter, Routes, Route, Navigate} from "react-router";
import './index.css'
import './i18n'
import AdminLayout from './admin/AdminLayout.jsx'
import User from './admin/User.jsx'
import OAuth2Client from './admin/OAuth2Client.jsx'

createRoot(document.getElementById('root')).render(
    <StrictMode>
        <BrowserRouter basename="/admin/console">
            <Routes>
                <Route path="/" element={<AdminLayout/>}>
                    <Route index element={<Navigate to="user" replace/>}/>
                    <Route path="user" element={<User/>}/>
                    <Route path="oauth2/client" element={<OAuth2Client/>}/>
                </Route>
            </Routes>
        </BrowserRouter>
    </StrictMode>,
)
