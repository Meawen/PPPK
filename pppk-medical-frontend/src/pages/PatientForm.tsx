import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { createPatient } from '../lib/api'
import type { CreatePatientRequest } from '../types'

export default function PatientForm() {
    const [form, setForm] = useState<CreatePatientRequest>({})
    const nav = useNavigate()

    const submit = async (e: React.FormEvent) => {
        e.preventDefault()
        const created = await createPatient(form)
        if (created?.id) nav(`/patients/${created.id}`)
    }

    const set = (k: keyof CreatePatientRequest) => (e: React.ChangeEvent<HTMLInputElement>) =>
        setForm(v => ({ ...v, [k]: e.target.value }))

    return (
        <div className="mx-auto max-w-3xl p-4">
            <h1 className="h1 mb-4">New Patient</h1>
            <form className="card space-y-4" onSubmit={submit}>
                <div><label className="block text-sm text-slate-600 mb-1">OIB</label><input className="input" onChange={set('oib')} /></div>
                <div className="grid grid-cols-2 gap-4">
                    <div><label className="block text-sm text-slate-600 mb-1">First name</label><input className="input" onChange={set('firstName')} /></div>
                    <div><label className="block text-sm text-slate-600 mb-1">Last name</label><input className="input" onChange={set('lastName')} /></div>
                </div>
                <div className="grid grid-cols-2 gap-4">
                    <div><label className="block text-sm text-slate-600 mb-1">Birth date</label><input type="date" className="input" onChange={set('birthDate')} /></div>
                    <div><label className="block text-sm text-slate-600 mb-1">Sex</label><input className="input" onChange={set('sex')} placeholder="M/F" /></div>
                </div>
                <div className="flex gap-2">
                    <button className="btn" type="submit">Create</button>
                    <button className="btn-outline" type="button" onClick={()=>nav(-1)}>Cancel</button>
                </div>
            </form>
        </div>
    )
}
