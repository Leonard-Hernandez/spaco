import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { appConfig } from '../config/app-config';
import {
  Appointment, AppointmentStatus, AvailabilityException, AvailabilityRule, Booking, BookingInput, Catalog, Category,
  ClinicalAccess, ClinicalRecord, Consent, ConsentTemplate, ConsentType, ContraindicationCheck, Member, NoteMatch,
  Patient, PatientInput, PatientSummary, PhotoKind, Professional, PublicTestimonial, RecordInput, Role, SessionInput,
  SessionNote, SessionPhoto, Slots, Staff, Tenant, Testimonial, TestimonialInput, Treatment, TreatmentInput
} from './models';

@Injectable({ providedIn: 'root' })
export class ApiService {

  private http = inject(HttpClient);
  private get base() {
    return `${appConfig().apiUrl}/api`;
  }

  /** Public photos come as /media/... relative to the API. */
  media(url: string | null): string | null {
    return url ? `${appConfig().apiUrl}${url}` : null;
  }

  // Public
  tenant(): Observable<Tenant> {
    return this.http.get<Tenant>(`${this.base}/public/tenant`);
  }
  catalog(): Observable<Catalog> {
    return this.http.get<Catalog>(`${this.base}/public/catalog`);
  }
  treatment(slug: string): Observable<Treatment> {
    return this.http.get<Treatment>(`${this.base}/public/treatments/${encodeURIComponent(slug)}`);
  }
  professionals(): Observable<Professional[]> {
    return this.http.get<Professional[]>(`${this.base}/public/professionals`);
  }
  availability(treatmentId: number, date: string, professionalId: number | null): Observable<Slots> {
    let params = new HttpParams().set('treatmentId', treatmentId).set('date', date);
    if (professionalId) params = params.set('professionalId', professionalId);
    return this.http.get<Slots>(`${this.base}/public/availability`, { params });
  }
  book(input: BookingInput): Observable<Booking> {
    return this.http.post<Booking>(`${this.base}/public/appointments`, input);
  }
  booking(token: string): Observable<Booking> {
    return this.http.get<Booking>(`${this.base}/public/appointments/${token}`);
  }
  cancelBooking(token: string): Observable<Booking> {
    return this.http.delete<Booking>(`${this.base}/public/appointments/${token}`);
  }
  consentText(type: ConsentType): Observable<ConsentTemplate> {
    return this.http.get<ConsentTemplate>(`${this.base}/public/consents/${type}`);
  }
  testimonials(): Observable<PublicTestimonial[]> {
    return this.http.get<PublicTestimonial[]>(`${this.base}/public/testimonials`);
  }

  // Patient portal (signed in)
  me(): Observable<Patient> {
    return this.http.get<Patient>(`${this.base}/me`);
  }
  myRecord(): Observable<ClinicalRecord> {
    return this.http.get<ClinicalRecord>(`${this.base}/me/record`);
  }
  mySessions(): Observable<SessionNote[]> {
    return this.http.get<SessionNote[]>(`${this.base}/me/sessions`);
  }
  myPhoto(id: number): Observable<Blob> {
    return this.http.get(`${this.base}/me/photos/${id}`, { responseType: 'blob' });
  }
  myAppointments(): Observable<Booking[]> {
    return this.http.get<Booking[]>(`${this.base}/me/appointments`);
  }
  myConsents(): Observable<Consent[]> {
    return this.http.get<Consent[]>(`${this.base}/me/consents`);
  }
  grantMyConsent(type: ConsentType): Observable<Consent> {
    return this.http.post<Consent>(`${this.base}/me/consents/${type}`, {});
  }
  revokeMyConsent(type: ConsentType): Observable<void> {
    return this.http.delete<void>(`${this.base}/me/consents/${type}`);
  }
  myTestimonials(): Observable<PublicTestimonial[]> {
    return this.http.get<PublicTestimonial[]>(`${this.base}/me/testimonials`);
  }
  myAccessLog(): Observable<ClinicalAccess[]> {
    return this.http.get<ClinicalAccess[]>(`${this.base}/me/access-log`);
  }

  // Panel (any staff)
  staff(): Observable<Staff> {
    return this.http.get<Staff>(`${this.base}/panel/me`);
  }
  pending(): Observable<Appointment[]> {
    return this.http.get<Appointment[]>(`${this.base}/panel/appointments/pending`);
  }
  appointments(from: string, to: string, status?: AppointmentStatus | null, professionalId?: number | null): Observable<Appointment[]> {
    let params = new HttpParams().set('from', from).set('to', to);
    if (status) params = params.set('status', status);
    if (professionalId) params = params.set('professionalId', professionalId);
    return this.http.get<Appointment[]>(`${this.base}/panel/appointments`, { params });
  }
  patientAppointments(patientId: number): Observable<Appointment[]> {
    return this.http.get<Appointment[]>(`${this.base}/panel/appointments/patient/${patientId}`);
  }
  decide(id: number, action: 'accept' | 'reject' | 'cancel' | 'complete' | 'no-show', reason?: string | null): Observable<Appointment> {
    return this.http.post<Appointment>(`${this.base}/panel/appointments/${id}/${action}`, reason ? { reason } : {});
  }
  staffProfessionals(): Observable<Professional[]> {
    return this.http.get<Professional[]>(`${this.base}/panel/professionals`);
  }
  searchPatients(q: string): Observable<Patient[]> {
    return this.http.get<Patient[]>(`${this.base}/panel/patients`, { params: { q } });
  }
  patient(id: number): Observable<Patient> {
    return this.http.get<Patient>(`${this.base}/panel/patients/${id}`);
  }
  savePatient(input: PatientInput, id?: number): Observable<Patient> {
    return id
      ? this.http.put<Patient>(`${this.base}/panel/patients/${id}`, input)
      : this.http.post<Patient>(`${this.base}/panel/patients`, input);
  }

  // Clinical (owner, esthetician)
  summary(patientId: number): Observable<PatientSummary> {
    return this.http.get<PatientSummary>(`${this.base}/panel/clinical/patients/${patientId}/summary`);
  }
  saveRecord(patientId: number, input: RecordInput): Observable<ClinicalRecord> {
    return this.http.put<ClinicalRecord>(`${this.base}/panel/clinical/patients/${patientId}/record`, input);
  }
  sessions(patientId: number): Observable<SessionNote[]> {
    return this.http.get<SessionNote[]>(`${this.base}/panel/clinical/patients/${patientId}/sessions`);
  }
  saveSession(patientId: number, input: SessionInput, id?: number): Observable<SessionNote> {
    return id
      ? this.http.put<SessionNote>(`${this.base}/panel/clinical/sessions/${id}`, input)
      : this.http.post<SessionNote>(`${this.base}/panel/clinical/patients/${patientId}/sessions`, input);
  }
  addPhoto(noteId: number, file: File, kind: PhotoKind, caption: string | null): Observable<SessionPhoto> {
    const form = new FormData();
    form.append('file', file);
    form.append('kind', kind);
    if (caption) form.append('caption', caption);
    return this.http.post<SessionPhoto>(`${this.base}/panel/clinical/sessions/${noteId}/photos`, form);
  }
  photo(id: number): Observable<Blob> {
    return this.http.get(`${this.base}/panel/clinical/photos/${id}`, { responseType: 'blob' });
  }
  deletePhoto(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/panel/clinical/photos/${id}`);
  }
  contraindications(patientId: number, treatmentId: number): Observable<ContraindicationCheck> {
    return this.http.get<ContraindicationCheck>(`${this.base}/panel/clinical/patients/${patientId}/contraindications`,
      { params: { treatmentId } });
  }
  searchNotes(q: string, patientId?: number): Observable<NoteMatch[]> {
    let params = new HttpParams().set('q', q);
    if (patientId) params = params.set('patientId', patientId);
    return this.http.get<NoteMatch[]>(`${this.base}/panel/clinical/search`, { params });
  }
  consents(patientId: number): Observable<Consent[]> {
    return this.http.get<Consent[]>(`${this.base}/panel/clinical/patients/${patientId}/consents`);
  }
  grantConsent(patientId: number, type: ConsentType, treatmentId: number | null): Observable<Consent> {
    return this.http.post<Consent>(`${this.base}/panel/clinical/patients/${patientId}/consents`, { type, treatmentId });
  }
  revokeConsent(patientId: number, type: ConsentType): Observable<void> {
    return this.http.delete<void>(`${this.base}/panel/clinical/patients/${patientId}/consents/${type}`);
  }
  accessLog(patientId: number): Observable<ClinicalAccess[]> {
    return this.http.get<ClinicalAccess[]>(`${this.base}/panel/clinical/patients/${patientId}/access-log`);
  }
  staffTestimonials(): Observable<Testimonial[]> {
    return this.http.get<Testimonial[]>(`${this.base}/panel/clinical/testimonials`);
  }
  saveTestimonial(input: TestimonialInput, id?: number): Observable<Testimonial> {
    return id
      ? this.http.put<Testimonial>(`${this.base}/panel/clinical/testimonials/${id}`, input)
      : this.http.post<Testimonial>(`${this.base}/panel/clinical/testimonials`, input);
  }
  testimonialAction(id: number, action: 'publish' | 'hide'): Observable<Testimonial> {
    return this.http.post<Testimonial>(`${this.base}/panel/clinical/testimonials/${id}/${action}`, {});
  }
  deleteTestimonial(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/panel/clinical/testimonials/${id}`);
  }

  // Admin (owner)
  categories(): Observable<Category[]> {
    return this.http.get<Category[]>(`${this.base}/panel/admin/categories`);
  }
  saveCategory(c: Omit<Category, 'id' | 'slug'>, id?: number): Observable<Category> {
    return id
      ? this.http.put<Category>(`${this.base}/panel/admin/categories/${id}`, c)
      : this.http.post<Category>(`${this.base}/panel/admin/categories`, c);
  }
  deleteCategory(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/panel/admin/categories/${id}`);
  }
  adminTreatments(): Observable<Treatment[]> {
    return this.http.get<Treatment[]>(`${this.base}/panel/admin/treatments`);
  }
  saveTreatment(t: TreatmentInput, id?: number): Observable<Treatment> {
    return id
      ? this.http.put<Treatment>(`${this.base}/panel/admin/treatments/${id}`, t)
      : this.http.post<Treatment>(`${this.base}/panel/admin/treatments`, t);
  }
  treatmentImage(id: number, file: File | null): Observable<Treatment> {
    if (!file) return this.http.delete<Treatment>(`${this.base}/panel/admin/treatments/${id}/image`);
    const form = new FormData();
    form.append('file', file);
    return this.http.put<Treatment>(`${this.base}/panel/admin/treatments/${id}/image`, form);
  }
  saveProfessional(p: Omit<Professional, 'id' | 'photoUrl'>, id?: number): Observable<Professional> {
    return id
      ? this.http.put<Professional>(`${this.base}/panel/admin/professionals/${id}`, p)
      : this.http.post<Professional>(`${this.base}/panel/admin/professionals`, p);
  }
  professionalPhoto(id: number, file: File): Observable<Professional> {
    const form = new FormData();
    form.append('file', file);
    return this.http.put<Professional>(`${this.base}/panel/admin/professionals/${id}/photo`, form);
  }
  rules(): Observable<AvailabilityRule[]> {
    return this.http.get<AvailabilityRule[]>(`${this.base}/panel/admin/schedule/rules`);
  }
  saveRule(r: Omit<AvailabilityRule, 'id'>, id?: number): Observable<AvailabilityRule> {
    return id
      ? this.http.put<AvailabilityRule>(`${this.base}/panel/admin/schedule/rules/${id}`, r)
      : this.http.post<AvailabilityRule>(`${this.base}/panel/admin/schedule/rules`, r);
  }
  deleteRule(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/panel/admin/schedule/rules/${id}`);
  }
  exceptions(from: string, to: string): Observable<AvailabilityException[]> {
    return this.http.get<AvailabilityException[]>(`${this.base}/panel/admin/schedule/exceptions`, { params: { from, to } });
  }
  addException(e: Omit<AvailabilityException, 'id'>): Observable<AvailabilityException> {
    return this.http.post<AvailabilityException>(`${this.base}/panel/admin/schedule/exceptions`, e);
  }
  deleteException(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/panel/admin/schedule/exceptions/${id}`);
  }
  members(): Observable<Member[]> {
    return this.http.get<Member[]>(`${this.base}/panel/admin/members`);
  }
  inviteMember(email: string, role: Role): Observable<Member> {
    return this.http.post<Member>(`${this.base}/panel/admin/members`, { email, role });
  }
  changeRole(id: number, role: Role): Observable<Member> {
    return this.http.put<Member>(`${this.base}/panel/admin/members/${id}`, { role });
  }
  removeMember(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/panel/admin/members/${id}`);
  }
  consentTemplates(): Observable<ConsentTemplate[]> {
    return this.http.get<ConsentTemplate[]>(`${this.base}/panel/admin/consent-templates`);
  }
  publishConsentTemplate(type: ConsentType, title: string, body: string): Observable<ConsentTemplate> {
    return this.http.post<ConsentTemplate>(`${this.base}/panel/admin/consent-templates`, { type, title, body });
  }
}

const MESSAGES: Record<string, string> = {
  SLOT_TAKEN: 'Ese horario se acaba de ocupar. Elige otro.',
  TOO_SOON: 'Ese horario ya está muy cerca. Elige uno más tarde.',
  PAST_START: 'Ese horario ya pasó.',
  TOO_MANY_PENDING: 'Ya tienes solicitudes esperando respuesta del spa.',
  INVALID_PHONE: 'Revisa el número de celular.',
  CONSENT_REQUIRED: 'Falta una autorización.',
  CONSENT_TEMPLATE_MISSING: 'El spa aún no publica ese texto de autorización.',
  PATIENT_NOT_LINKED: 'No encontramos una paciente con el correo de esta cuenta.',
  PHONE_TAKEN: 'Otra paciente ya usa ese número.',
  INVALID_IMAGE: 'Sube una foto JPG, PNG o WebP de máximo 8 MB.',
  INVALID_APPOINTMENT_STATE: 'La cita ya cambió de estado. Recarga la página.',
  SESSION_EXISTS: 'Esa cita ya tiene su nota de sesión.',
  DUPLICATE_NAME: 'Ya existe uno con ese nombre.',
  CATEGORY_NOT_EMPTY: 'Mueve o borra primero sus tratamientos.',
  MEMBER_EXISTS: 'Ese correo ya está en el equipo.',
  SELF_CHANGE: 'No puedes quitarte ni bajarte el rol a ti misma.',
  INVALID_TIME_RANGE: 'La hora de inicio debe ser antes que la de fin.',
  VALIDATION_ERROR: 'Revisa los datos del formulario.'
};

/** Spanish message for an API error. */
export function errorMessage(e: unknown, fallback = 'Algo salió mal. Intenta de nuevo.'): string {
  if (e instanceof HttpErrorResponse) {
    if (e.status === 0) return 'Sin conexión con el servidor.';
    const code = (e.error as { code?: string } | null)?.code;
    if (code && MESSAGES[code]) return MESSAGES[code];
    if (e.status === 403) return 'Tu rol no tiene acceso a esto.';
  }
  return fallback;
}
