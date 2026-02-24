import { format, startOfWeek, startOfMonth, parseISO } from 'date-fns';

export interface PriceData {
  time: string;
  open: number;
  high: number;
  low: number;
  close: number;
  value: number; // Volume
}

/**
 * 일봉 데이터를 주봉/월봉으로 집계
 */
export function aggregateData(data: PriceData[], period: 'D' | 'W' | 'M') {
  if (period === 'D') return data;

  const result: PriceData[] = [];
  const groups: { [key: string]: PriceData[] } = {};

  data.forEach((item) => {
    const date = parseISO(item.time);
    const key = period === 'W' 
      ? format(startOfWeek(date, { weekStartsOn: 1 }), 'yyyy-MM-dd')
      : format(startOfMonth(date), 'yyyy-MM-dd');
    
    if (!groups[key]) groups[key] = [];
    groups[key].push(item);
  });

  Object.keys(groups).sort().forEach((key) => {
    const group = groups[key];
    result.push({
      time: key,
      open: group[0].open,
      high: Math.max(...group.map(g => g.high)),
      low: Math.min(...group.map(g => g.low)),
      close: group[group.length - 1].close,
      value: group.reduce((sum, g) => sum + g.value, 0),
    });
  });

  return result;
}

/**
 * 이동평균선(MA) 계산
 */
export function calculateMA(data: PriceData[], period: number) {
  const result = [];
  for (let i = 0; i < data.length; i++) {
    if (i < period - 1) {
      continue;
    }
    const slice = data.slice(i - period + 1, i + 1);
    const sum = slice.reduce((acc, curr) => acc + curr.close, 0);
    result.push({
      time: data[i].time,
      value: sum / period,
    });
  }
  return result;
}
