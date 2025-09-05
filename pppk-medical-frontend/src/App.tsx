import { Route, Routes, Link } from 'react-router-dom'
import Nav from './components/Nav'
import PatientsList from './pages/PatientsList'
import PatientForm from './pages/PatientForm'
import PatientDetail from './pages/PatientDetail'

export default function App() {
    return (
        <div className="min-h-dvh">
            <Nav />
            <Routes>
                <Route path="/" element={<PatientsList />} />
                <Route path="/patients/new" element={<PatientForm />} />
                <Route path="/patients/:id" element={<PatientDetail />} />
                <Route path="*" element={<NotFound />} />
            </Routes>
        </div>
    )
}

function NotFound() {
    return (
        <div className="p-6">
            <div className="h1 mb-2">Not found</div>
            <Link to="/" className="text-teal-700">Go home</Link>
        </div>
    )
}
