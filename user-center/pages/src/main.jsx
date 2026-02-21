import {StrictMode} from 'react'
import {createRoot} from 'react-dom/client'
import {BrowserRouter, Routes, Route} from "react-router";
//import './index.css'
import User from './admin/User.jsx'

createRoot(document.getElementById('root')).render(
    <StrictMode>
        <BrowserRouter>
            <Routes>
                <Route path="/admin/user" element={<User />} />
            </Routes>
        </BrowserRouter>
    </StrictMode>,
)
