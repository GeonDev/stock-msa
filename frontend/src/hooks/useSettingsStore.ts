import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface SettingsState {
  theme: 'dark' | 'light' | 'system';
  setTheme: (theme: 'dark' | 'light' | 'system') => void;
  
  // 차트 표시 옵션
  chartConfig: {
    isCandleMode: boolean;
    showMA5: boolean;
    showMA20: boolean;
    showMA60: boolean;
    showMA120: boolean;
    showVolume: boolean;
  };
  setChartConfig: (config: Partial<SettingsState['chartConfig']>) => void;

  // Phase 4.2: 전략 기본값 설정
  defaults: {
    tradingFeeRate: number;
    taxRate: number;
    maxWeightPerStock: number;
  };
  setDefaults: (defaults: SettingsState['defaults']) => void;
}

export const useSettingsStore = create<SettingsState>()(
  persist(
    (set) => ({
      theme: 'dark',
      setTheme: (theme) => {
        set({ theme });
        const root = window.document.documentElement;
        root.classList.remove('light', 'dark');
        
        if (theme === 'system') {
          const systemTheme = window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
          root.classList.add(systemTheme);
        } else {
          root.classList.add(theme);
        }
      },
      chartConfig: {
        isCandleMode: false,
        showMA5: true,
        showMA20: true,
        showMA60: false,
        showMA120: false,
        showVolume: true,
      },
      setChartConfig: (config) => set((state) => ({ 
        chartConfig: { ...state.chartConfig, ...config } 
      })),
      defaults: {
        tradingFeeRate: 0.0015,
        taxRate: 0.002,
        maxWeightPerStock: 0.2,
      },
      setDefaults: (defaults) => set({ defaults }),
    }),
    { name: 'stock-msa-settings' }
  )
);
