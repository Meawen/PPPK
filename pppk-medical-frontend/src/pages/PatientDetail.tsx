import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
    getPatient, updatePatient, deletePatient,
    listExams, listExamTypes, createExam,
    addHistory, prescribe, listAttachments, addAttachment,
    listHistory, listPrescriptions
} from '../lib/api'
import type {
    PatientResponse, UpdatePatientRequest, ExamResponse, ExamTypeResponse,
    HistoryRequest, PrescriptionRequest, AttachmentResponse, AttachmentRequest, CreateExamRequest
} from '../types'

export default function PatientDetail() {
    const { id } = useParams()
    const pid = useMemo(() => Number(id), [id])
    const nav = useNavigate()

    const [p, setP] = useState<PatientResponse | null>(null)
    const [edit, setEdit] = useState<UpdatePatientRequest>({})
    const [exams, setExams] = useState<ExamResponse[]>([])
    const [types, setTypes] = useState<ExamTypeResponse[]>([])
    const [examForm, setExamForm] = useState<CreateExamRequest>({ patientId: pid })
    const [hist, setHist] = useState<HistoryRequest>({ diseaseName: '', startDate: '', endDate: '' })
    const [rx, setRx] = useState<PrescriptionRequest>({ medication: '', dosage: '', instructions: '' })
    const [selectedExamId, setSelectedExamId] = useState<number | null>(null)
    const [atts, setAtts] = useState<AttachmentResponse[]>([])
    const [attForm, setAttForm] = useState<AttachmentRequest>({})
    const [attErr, setAttErr] = useState('')
    const [historyList, setHistoryList] = useState<any[]>([])
    const [rxList, setRxList] = useState<any[]>([])
    const [histErr, setHistErr] = useState('')
    const [rxErr, setRxErr] = useState('')

    // helpers
    const toArray = (x: any): any[] =>
        Array.isArray(x) ? x :
            Array.isArray(x?.content) ? x.content :
                Array.isArray(x?.items) ? x.items : []

    const opt = (s?: string) => (s && s.trim() !== '' ? s : undefined)
    const dateOnly = (s?: string) =>
        !s ? undefined : /^\d{4}-\d{2}-\d{2}$/.test(s) ? s : new Date(s).toISOString().slice(0,10)

    const fileToSha256Hex = async (file: File) => {
        const buf = await file.arrayBuffer()
        const hash = await crypto.subtle.digest('SHA-256', buf)
        return Array.from(new Uint8Array(hash)).map(b=>b.toString(16).padStart(2,'0')).join('')
    }

    const onPickFile: React.ChangeEventHandler<HTMLInputElement> = async (e) => {
        const f = e.target.files?.[0]
        if (!f) return
        const sha = await fileToSha256Hex(f)
        setAttForm(v => ({
            ...v,
            objectKey: v.objectKey || `exams/${selectedExamId ?? 'unknown'}/${Date.now()}_${f.name}`,
            contentType: f.type || 'application/octet-stream',
            sizeBytes: f.size,
            sha256Hex: sha,
        }))
    }

    const doAttach = async () => {
        try {
            setAttErr('')
            if (!selectedExamId) return
            if (!attForm.objectKey || !attForm.contentType || !attForm.sizeBytes || Number(attForm.sizeBytes) <= 0) {
                setAttErr('objectKey, contentType, sizeBytes > 0 are required'); return
            }
            await addAttachment(selectedExamId, attForm)
            const aRaw = await listAttachments(selectedExamId)
            setAtts(toArray(aRaw) as AttachmentResponse[])
            setAttForm({})
        } catch (e:any) {
            setAttErr(e?.response?.data?.message || String(e))
        }
    }

    const load = async () => {
        const [patient, exRaw, tRaw, hRaw, rxRaw] = await Promise.all([
            getPatient(pid),
            listExams(pid, 0, 50),
            listExamTypes(),
            listHistory(pid),
            listPrescriptions(pid),
        ])
        setP(patient)
        setEdit({
            firstName: patient.firstName,
            lastName: patient.lastName,
            birthDate: patient.birthDate,
            sex: patient.sex
        })
        const ex = toArray(exRaw) as ExamResponse[]
        setExams(ex)
        setTypes(toArray(tRaw) as ExamTypeResponse[])
        setHistoryList(Array.isArray(hRaw) ? hRaw : [])
        setRxList(Array.isArray(rxRaw) ? rxRaw : [])

        if (ex.length) {
            const firstId = ex[0].id!
            setSelectedExamId(firstId)
            const aRaw = await listAttachments(firstId)
            setAtts(toArray(aRaw) as AttachmentResponse[])
        } else {
            setSelectedExamId(null)
            setAtts([])
        }
    }

    useEffect(() => { load() /* eslint-disable-next-line */ }, [pid])

    const save = async () => { if (!p?.id) return; setP(await updatePatient(p.id, edit)) }
    const destroy = async () => { if (!p?.id) return; await deletePatient(p.id); nav('/') }

    const addExamClick = async () => {
        const created = await createExam({ ...examForm, patientId: pid })
        if (created?.id) {
            setExams(prev => [created, ...(Array.isArray(prev) ? prev : [])])
            setSelectedExamId(created.id!)
            const aRaw = await listAttachments(created.id!)
            setAtts(toArray(aRaw) as AttachmentResponse[])
        }
    }

    const addHist = async () => {
        try {
            setHistErr('')
            if (!hist.diseaseName?.trim()) { setHistErr('Disease name is required'); return }
            if (!hist.startDate) { setHistErr('Start date is required'); return } // backend @NotNull
            await addHistory(pid, {
                diseaseName: hist.diseaseName.trim(),
                startDate: dateOnly(hist.startDate)!,             // REQUIRED
                endDate: dateOnly(hist.endDate)                   // optional
            })
            setHistoryList(await listHistory(pid))
            setHist({ diseaseName: '', startDate: '', endDate: '' })
        } catch (e:any) {
            setHistErr(e?.response?.data?.message || 'Failed to add history')
        }
    }

    const addRx = async () => {
        try {
            setRxErr('')
            if (!rx.medication?.trim()) { setRxErr('Medication is required'); return }
            await prescribe(pid, {
                medication: rx.medication.trim(),
                dosage: opt(rx.dosage),
                instructions: opt(rx.instructions)
            })
            setRxList(await listPrescriptions(pid))
            setRx({ medication: '', dosage: '', instructions: '' })
        } catch (e:any) {
            setRxErr(e?.response?.data?.message || 'Failed to add prescription')
        }
    }

    const chooseExam = async (eid: number) => {
        setSelectedExamId(eid)
        const aRaw = await listAttachments(eid)
        setAtts(toArray(aRaw) as AttachmentResponse[])
    }

    if (!p) return <div className="p-4">Loading…</div>

    return (
        <div className="mx-auto max-w-6xl p-4 space-y-6">
            <div className="flex items-center justify-between">
                <h1 className="h1">Patient #{p.id}</h1>
                <div className="flex gap-2">
                    <button className="btn" onClick={save}>Save</button>
                    <button className="btn-outline" onClick={destroy}>Delete</button>
                    <Link className="btn-outline" to="/">Back</Link>
                </div>
            </div>

            <div className="grid md:grid-cols-2 gap-6">
                <div className="card space-y-3">
                    <div className="grid grid-cols-2 gap-3">
                        <L label="OIB"><span className="mono">{p.oib}</span></L>
                        <L label="Birth"><input className="input" value={edit.birthDate||''} onChange={e=>setEdit(v=>({...v, birthDate:e.target.value}))}/></L>
                        <L label="First name"><input className="input" value={edit.firstName||''} onChange={e=>setEdit(v=>({...v, firstName:e.target.value}))}/></L>
                        <L label="Last name"><input className="input" value={edit.lastName||''} onChange={e=>setEdit(v=>({...v, lastName:e.target.value}))}/></L>
                        <L label="Sex"><input className="input" value={edit.sex||''} onChange={e=>setEdit(v=>({...v, sex:e.target.value}))}/></L>
                    </div>
                </div>

                <div className="card space-y-3">
                    <div className="h2">Add History</div>
                    {histErr && <div className="text-red-600 text-sm">{histErr}</div>}
                    <div className="grid grid-cols-2 gap-3">
                        <input className="input" placeholder="Disease name" value={hist.diseaseName||''}
                               onChange={e=>setHist(v=>({...v, diseaseName:e.target.value}))}/>
                        <input className="input" type="date" value={hist.startDate||''}
                               onChange={e=>setHist(v=>({...v, startDate:e.target.value}))}/>
                        <input className="input" type="date" value={hist.endDate||''}
                               onChange={e=>setHist(v=>({...v, endDate:e.target.value}))}/>
                        <button className="btn" onClick={addHist}>Add</button>
                    </div>

                    <div className="h2 mt-4">Add Prescription</div>
                    {rxErr && <div className="text-red-600 text-sm">{rxErr}</div>}
                    <div className="grid grid-cols-3 gap-3">
                        <input className="input" placeholder="Medication" value={rx.medication||''}
                               onChange={e=>setRx(v=>({...v, medication:e.target.value}))}/>
                        <input className="input" placeholder="Dosage" value={rx.dosage||''}
                               onChange={e=>setRx(v=>({...v, dosage:e.target.value}))}/>
                        <input className="input" placeholder="Instructions" value={rx.instructions||''}
                               onChange={e=>setRx(v=>({...v, instructions:e.target.value}))}/>
                        <button className="btn" onClick={addRx} disabled={!rx.medication?.trim()}>Prescribe</button>
                    </div>
                </div>
            </div>

            <div className="card space-y-2">
                <div className="h2">History</div>
                <table className="min-w-full text-sm"><tbody>
                {(historyList||[]).map((h,i)=>(
                    <tr key={i} className="border-t"><td className="py-2">
                        {h.diseaseName} — {h.startDate} → {h.endDate ?? '…'}
                    </td></tr>
                ))}
                {!historyList?.length && <tr><td className="py-4 text-slate-500">No history.</td></tr>}
                </tbody></table>
            </div>

            <div className="card space-y-2">
                <div className="h2">Prescriptions</div>
                <table className="min-w-full text-sm"><tbody>
                {(rxList||[]).map((r,i)=>(
                    <tr key={i} className="border-t"><td className="py-2">
                        {r.medication} • {r.dosage} — {r.instructions}
                    </td></tr>
                ))}
                {!rxList?.length && <tr><td className="py-4 text-slate-500">No prescriptions.</td></tr>}
                </tbody></table>
            </div>

            <div className="card space-y-4">
                <div className="flex items-center justify-between">
                    <div className="h2">Exams</div>
                    <div className="flex gap-2">
                        <select className="input" onChange={e=>setExamForm(v=>({...v, examType:e.target.value}))} defaultValue="">
                            <option value="" disabled>Select exam type</option>
                            {types.map(t=> <option key={t.code} value={t.code}>{t.name} ({t.code})</option>)}
                        </select>
                        <input className="input" placeholder="Notes" onChange={e=>setExamForm(v=>({...v, notes:e.target.value}))}/>
                        <input className="input" type="datetime-local" onChange={e=>setExamForm(v=>({...v, occurredAt:e.target.value}))}/>
                        <button className="btn" onClick={addExamClick}>Add Exam</button>
                    </div>
                </div>

                <div className="overflow-x-auto">
                    <table className="min-w-full text-sm">
                        <thead className="text-left text-slate-500">
                        <tr><th className="py-2">ID</th><th>Occurred</th><th>Type</th><th>Notes</th><th></th></tr>
                        </thead>
                        <tbody>
                        {(Array.isArray(exams) ? exams : []).map(e=>(
                            <tr key={e.id} className="border-t">
                                <td className="py-2">{e.id}</td>
                                <td>{e.occurredAt?.replace('T',' ')}</td>
                                <td>{e.examType}</td>
                                <td>{e.notes}</td>
                                <td><button className="text-teal-700" onClick={()=>chooseExam(e.id!)}>Attachments</button></td>
                            </tr>
                        ))}
                        {(!Array.isArray(exams) || exams.length === 0) && (
                            <tr><td colSpan={5} className="py-6 text-center text-slate-500">No exams.</td></tr>
                        )}
                        </tbody>
                    </table>
                </div>

                {selectedExamId && (
                    <div className="grid md:grid-cols-2 gap-4">
                        <div className="space-y-2">
                            <div className="h2">Attachments for exam #{selectedExamId}</div>
                            <div className="overflow-x-auto">
                                <table className="min-w-full text-sm">
                                    <thead className="text-left text-slate-500">
                                    <tr><th className="py-2">ID</th><th>Object Key</th><th>Type</th><th>Size</th></tr>
                                    </thead>
                                    <tbody>
                                    {(Array.isArray(atts) ? atts : []).map(a=>(
                                        <tr key={a.id} className="border-t">
                                            <td className="py-2">{a.id}</td>
                                            <td className="mono">{a.objectKey}</td>
                                            <td>{a.contentType}</td>
                                            <td>{a.sizeBytes}</td>
                                        </tr>
                                    ))}
                                    {(!Array.isArray(atts) || atts.length === 0) && (
                                        <tr><td colSpan={4} className="py-6 text-center text-slate-500">No attachments.</td></tr>
                                    )}
                                    </tbody>
                                </table>
                            </div>
                        </div>
                        <div className="space-y-2">
                            <div className="h2">Add Attachment</div>
                            {attErr && <div className="text-red-600 text-sm">{attErr}</div>}
                            <div className="grid grid-cols-2 gap-3">
                                <input className="input" type="file" onChange={onPickFile} />
                                <input className="input" placeholder="s3 key (optional override)" value={attForm.objectKey||''}
                                       onChange={e=>setAttForm(v=>({...v, objectKey:e.target.value}))}/>
                                <input className="input" placeholder="content/type" value={attForm.contentType||''}
                                       onChange={e=>setAttForm(v=>({...v, contentType:e.target.value}))}/>
                                <input className="input" type="number" placeholder="size bytes" value={attForm.sizeBytes ?? ''}
                                       onChange={e=>setAttForm(v=>({...v, sizeBytes:Number(e.target.value)}))}/>
                                <input className="input" placeholder="sha256 hex" value={attForm.sha256Hex||''}
                                       onChange={e=>setAttForm(v=>({...v, sha256Hex:e.target.value}))}/>
                                <button className="btn" onClick={doAttach}>Attach</button>
                            </div>
                        </div>
                    </div>
                )}
            </div>
        </div>
    )
}

function L({ label, children }: { label: string; children: React.ReactNode }) {
    return (
        <label className="block">
            <div className="text-sm text-slate-600 mb-1">{label}</div>
            {children}
        </label>
    )
}
