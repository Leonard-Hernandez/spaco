import { openingHoursLines, OpeningDay } from './opening-hours';

const day = (weekday: number, start: string, end: string): OpeningDay => ({ weekday, blocks: [{ start, end }] });

describe('openingHoursLines', () => {
  it('groups consecutive days with the same hours', () => {
    const days = [day(1, '13:00', '18:00'), day(2, '13:00', '18:00'),
      day(3, '08:00', '14:00'), day(4, '08:00', '14:00'), day(5, '08:00', '14:00'), day(6, '08:00', '14:00'), day(7, '08:00', '14:00')];
    expect(openingHoursLines(days)).toEqual([
      'Lun y mar: 1:00 p. m. – 6:00 p. m.',
      'Mié a dom: 8:00 a. m. – 2:00 p. m.'
    ]);
  });

  it('does not list closed days at the ends of the week', () => {
    expect(openingHoursLines([day(2, '09:00', '12:00'), day(3, '09:00', '12:00'), day(4, '09:00', '12:00')]))
      .toEqual(['Mar a jue: 9:00 a. m. – 12:00 p. m.']);
  });

  it('lists a closed day that sits between open days', () => {
    expect(openingHoursLines([day(1, '09:00', '12:00'), day(3, '09:00', '12:00')])).toEqual([
      'Lun: 9:00 a. m. – 12:00 p. m.', 'Mar: cerrado', 'Mié: 9:00 a. m. – 12:00 p. m.'
    ]);
  });

  it('joins split blocks of one day', () => {
    expect(openingHoursLines([{ weekday: 6, blocks: [{ start: '08:00', end: '12:00' }, { start: '14:00', end: '18:00' }] }]))
      .toEqual(['Sáb: 8:00 a. m. – 12:00 p. m., 2:00 p. m. – 6:00 p. m.']);
  });

  it('returns nothing when the spa has no hours', () => {
    expect(openingHoursLines([])).toEqual([]);
  });
});
