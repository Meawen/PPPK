import { Link, useLocation } from 'react-router-dom'
export default function Nav() {
    const loc = useLocation()
    const active = (p: string) => loc.pathname.startsWith(p) ? 'text-teal-700 font-semibold' : 'text-slate-600'
    return (
        <div className="sticky top-0 z-10 bg-white border-b border-slate-200">
            <div className="mx-auto max-w-6xl px-4 py-3 flex items-center justify-between">
                <Link to="/" className="text-lg font-semibold text-teal-700">PPPK Medical</Link>
                <div className="flex gap-6">
                    <Link className={active('/')} to="/">Patients</Link>
                </div>
            </div>
        </div>
    )
}