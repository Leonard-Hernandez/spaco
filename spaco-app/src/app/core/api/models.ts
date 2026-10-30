/** spaco API contracts (see the backend DTOs). Prices are whole pesos; null = not published. */

export interface Tenant {
  slug: string;
  name: string;
  timezone: string;
  contactPhone: string | null;
  address: string | null;
  branding: Branding;
}

/** Public profile the owner keeps in the tenant row; every field is optional. */
export interface Branding {
  wordmark?: string;
  subline?: string;
  /** Logo mark shown next to the wordmark (URL); the honeycomb is used when absent */
  logoMark?: string;
  tagline?: string;
  intro?: string;
  city?: string;
  hours?: string;
  address?: string;
  whatsapp?: string;
  email?: string;
  instagram?: string;
  /** Treatment slug shown on the hero label */
  heroTreatment?: string;
  cancellationNote?: string;
  /** CSS token overrides, e.g. { "amber": "#d08a1c" } */
  theme?: Record<string, string>;
}

export type Ink = 'honey' | 'lavender' | 'sage' | 'navy' | 'clay';

export interface Category {
  id: number;
  name: string;
  slug: string;
  ink: Ink;
  position: number;
  active: boolean;
}

export interface CompositionLine {
  label: string;
  value: string;
}

export interface Treatment {
  id: number;
  categoryId: number;
  category: string;
  categoryName: string;
  formulaNo: number | null;
  name: string;
  slug: string;
  tagline: string | null;
  description: string | null;
  benefits: string[];
  composition: CompositionLine[];
  contraindications: string | null;
  durationMin: number;
  bufferMin: number;
  price: number | null;
  imageUrl: string | null;
  position: number;
  active: boolean;
}

export interface TreatmentInput {
  categoryId: number;
  formulaNo: number | null;
  name: string;
  tagline: string | null;
  description: string | null;
  benefits: string[];
  composition: CompositionLine[];
  contraindications: string | null;
  durationMin: number;
  bufferMin: number;
  price: number | null;
  position: number;
  active: boolean;
}

export interface Catalog {
  categories: Category[];
  treatments: Treatment[];
}

export interface Professional {
  id: number;
  displayName: string;
  title: string | null;
  bio: string | null;
  photoUrl: string | null;
  memberId: number | null;
  active: boolean;
}

export interface Slots {
  date: string;
  treatmentId: number;
  professionalId: number | null;
  durationMin: number;
  /** "09:30:00", spa local time */
  slots: string[];
}

export type AppointmentStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'CANCELLED' | 'COMPLETED' | 'NO_SHOW';

export interface BookingInput {
  treatmentId: number;
  professionalId: number | null;
  startAt: string;
  fullName: string;
  phone: string;
  email: string | null;
  note: string | null;
  acceptsDataPolicy: boolean;
  website: string;
}

/** What the patient sees of her booking; token = her link to manage it. */
export interface Booking {
  token: string;
  treatmentId: number;
  treatmentName: string;
  professionalName: string;
  patientName: string;
  startAt: string;
  endAt: string;
  status: AppointmentStatus;
  rejectReason: string | null;
}

export interface Appointment {
  id: number;
  treatmentId: number;
  treatmentName: string;
  professionalId: number;
  professionalName: string;
  patientId: number;
  patientName: string;
  patientPhone: string;
  startAt: string;
  endAt: string;
  status: AppointmentStatus;
  patientNote: string | null;
  decidedAt: string | null;
  rejectReason: string | null;
  createdAt: string;
}

export type ConsentType = 'DATA_PROCESSING' | 'TREATMENT' | 'PHOTO_TESTIMONIAL';

export interface ConsentTemplate {
  id: number;
  type: ConsentType;
  version: number;
  title: string;
  body: string;
  active: boolean;
}

export interface Consent {
  id: number;
  patientId: number;
  type: ConsentType;
  templateVersion: number;
  treatmentId: number | null;
  acceptedAt: string;
  revokedAt: string | null;
  channel: 'WEB' | 'PORTAL' | 'IN_PERSON';
  recordedBy: string | null;
}

export interface Patient {
  id: number;
  fullName: string;
  phone: string;
  email: string | null;
  birthDate: string | null;
  portalLinked: boolean;
  createdAt: string;
}

export interface PatientInput {
  fullName: string;
  phone: string;
  email: string | null;
  birthDate: string | null;
}

export interface ClinicalRecord {
  patientId: number;
  allergies: string | null;
  medications: string | null;
  conditions: string | null;
  pregnancy: string | null;
  skinType: string | null;
  phototype: number | null;
  surgeries: string | null;
  habits: string | null;
  observations: string | null;
  updatedBy: string | null;
  updatedAt: string | null;
}

export type RecordInput = Omit<ClinicalRecord, 'patientId' | 'updatedBy' | 'updatedAt'>;

export type PhotoKind = 'BEFORE' | 'AFTER' | 'PROGRESS';

export interface SessionPhoto {
  id: number;
  sessionNoteId: number;
  kind: PhotoKind;
  caption: string | null;
  createdAt: string;
}

export interface SessionNote {
  id: number;
  patientId: number;
  appointmentId: number | null;
  treatmentId: number | null;
  treatmentName: string | null;
  professionalId: number | null;
  professionalName: string | null;
  performedAt: string;
  products: string | null;
  parameters: string | null;
  observations: string | null;
  recommendations: string | null;
  createdBy: string | null;
  photos: SessionPhoto[];
}

export interface SessionInput {
  appointmentId: number | null;
  treatmentId: number | null;
  professionalId: number | null;
  performedAt: string | null;
  products: string | null;
  parameters: string | null;
  observations: string | null;
  recommendations: string | null;
}

export interface PatientSummary {
  patient: { id: number; fullName: string; phoneE164: string; email: string | null; birthDate: string | null };
  age: number | null;
  record: ClinicalRecord;
  alerts: string[];
  recentSessions: SessionNote[];
  upcoming: Appointment[];
  validConsents: ConsentType[];
}

export interface ContraindicationCheck {
  patientId: number;
  patientName: string;
  treatmentId: number;
  treatmentName: string;
  contraindications: string | null;
  matches: string[];
  alerts: string[];
  recordFilled: boolean;
  treatmentConsentSigned: boolean;
}

export interface NoteMatch {
  sessionNoteId: number;
  patientId: number;
  patientName: string;
  performedAt: string;
  treatmentName: string | null;
  excerpt: string;
}

export interface ClinicalAccess {
  id: number;
  patientId: number | null;
  actorSub: string;
  actorName: string | null;
  channel: 'PANEL' | 'PORTAL' | 'MCP';
  action: string;
  at: string;
}

export type TestimonialStatus = 'DRAFT' | 'PUBLISHED' | 'HIDDEN';

export interface Testimonial {
  id: number;
  patientId: number;
  displayName: string;
  quote: string | null;
  treatmentId: number | null;
  treatmentName: string | null;
  status: TestimonialStatus;
  publishedAt: string | null;
  photos: { id: number; sessionPhotoId: number; kind: PhotoKind; position: number; publicFile: string | null }[];
}

export interface TestimonialInput {
  patientId: number;
  displayName: string;
  quote: string | null;
  treatmentId: number | null;
  photoIds: number[];
}

export interface PublicTestimonial {
  id: number;
  displayName: string;
  quote: string | null;
  treatmentName: string | null;
  publishedAt: string;
  photos: { kind: PhotoKind; url: string }[];
}

export type Role = 'OWNER' | 'ESTHETICIAN' | 'RECEPTION';

export interface Staff {
  email: string;
  name: string | null;
  role: Role;
}

export interface Member {
  id: number;
  email: string;
  name: string | null;
  role: Role;
  linked: boolean;
}

export interface AvailabilityRule {
  id: number;
  professionalId: number;
  weekday: 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';
  startTime: string;
  endTime: string;
  active: boolean;
}

export interface AvailabilityException {
  id: number;
  professionalId: number | null;
  date: string;
  startTime: string | null;
  endTime: string | null;
  type: 'BLOCK' | 'EXTRA';
  note: string | null;
}

export interface ApiError {
  code: string;
  message: string;
}
