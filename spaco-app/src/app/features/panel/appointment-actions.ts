import { Appointment } from '../../core/api/models';

/** What the staff can do with an appointment right now. */
export function actionsFor(a: Appointment, now = Date.now()) {
  const started = new Date(a.startAt).getTime() <= now;
  return {
    accept: a.status === 'PENDING',
    reject: a.status === 'PENDING',
    cancel: a.status === 'ACCEPTED' && !started,
    complete: a.status === 'ACCEPTED' || a.status === 'PENDING',
    noShow: a.status === 'ACCEPTED' && started
  };
}
