import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { searchPatients, byOib, exportPatientsCsvUrl } from '../lib/api'
import type { PatientResponse } from '../types'

export default function PatientsList() {
    const [surname, setSurname] = useState('')
    const [oib, setOib] = useState('')
    const [rows, setRows] = useState<PatientResponse[]>([])
    const [loading, setLoading] = useState(false)
    const nav = useNavigate()

    const run = async () => {
        setLoading(true)
        try {
            const data = await searchPatients(surname || undefined, 0, 50)
            setRows(Array.isArray(data) ? data : [])
        } finally { setLoading(false) }
    }

    useEffect(() => { run() }, [])

    const findByOib = async () => {
        if (!oib) return
        const p = await byOib(oib)
        if (p?.id) nav(`/patients/${p.id}`)
    }

    const list = Array.isArray(rows) ? rows : []

    return (
        <div className="mx-auto max-w-6xl p-4">
            <div className="flex items-end gap-3">
                <div className="flex-1">
                    <label className="block text-sm text-slate-600 mb-1">Search by surname</label>
                    <input className="input" value={surname} onChange={e=>setSurname(e.target.value)} placeholder="e.g. Horvat" />
                </div>
                <button className="btn" onClick={run} disabled={loading}>Search</button>
                <a className="btn-outline" href={exportPatientsCsvUrl()} target="_blank" rel="noreferrer">Export CSV</a>
                <Link to="/patients/new" className="btn">New Patient</Link>
            </div>

            <div className="mt-6 card">
                <div className="flex items-end gap-3">
                    <div>
                        <label className="block text-sm text-slate-600 mb-1">Find by OIB</label>
                        <input className="input" value={oib} onChange={e=>setOib(e.target.value)} placeholder="e.g. 12345678901" />
                    </div>
                    <button className="btn" onClick={findByOib}>Go</button>
                </div>

                <div className="mt-6 overflow-x-auto">
                    <table className="min-w-full text-sm">
                        <thead className="text-left text-slate-500">
                        <tr><th className="py-2">ID</th><th>OIB</th><th>Name</th><th>Birth</th><th>Sex</th><th></th></tr>
                        </thead>
                        <tbody>
                        {list.map(p=>(
                            <tr key={p.id} className="border-t">
                                <td className="py-2">{p.id}</td>
                                <td>{p.oib}</td>
                                <td>{p.firstName} {p.lastName}</td>
                                <td>{p.birthDate}</td>
                                <td>{p.sex}</td>
                                <td><Link to={`/patients/${p.id}`} className="text-teal-700">Open</Link></td>
                            </tr>
                        ))}
                        {!list.length && !loading && (
                            <tr><td colSpan={6} className="py-6 text-center text-slate-500">No results.</td></tr>
                        )}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    )
}
