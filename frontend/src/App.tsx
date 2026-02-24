import { Routes, Route, Link, useLocation } from 'react-router-dom';
import { Home, LineChart, LayoutDashboard, Settings as SettingsIcon, CandlestickChart } from 'lucide-react';
import DashboardHome from './pages/DashboardHome';
import BacktestSimulator from './pages/BacktestSimulator';
import StrategyCompare from './pages/StrategyCompare';
import Settings from './pages/Settings';
import StockDetail from './pages/StockDetail';
import StockSearch from './components/StockSearch';
import { useSettingsStore } from './hooks/useSettingsStore';
import { useEffect } from 'react';

const Layout = ({ children }: { children: React.ReactNode }) => {
  const location = useLocation();
  const { theme, setTheme } = useSettingsStore();

  useEffect(() => {
    // 초기 테마 적용
    const root = window.document.documentElement;
    root.classList.remove('light', 'dark');
    if (theme === 'system') {
      const systemTheme = window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
      root.classList.add(systemTheme);
    } else {
      root.classList.add(theme);
    }
  }, [theme]);

  const navItems = [
    { path: '/', name: 'Home', icon: Home },
    { path: '/stock/005930', name: 'Charts', icon: CandlestickChart },
    { path: '/simulator', name: 'Simulator', icon: LineChart },
    { path: '/compare', name: 'Compare', icon: LayoutDashboard },
    { path: '/settings', name: 'Settings', icon: SettingsIcon },
  ];

  return (
    <div className="flex h-screen bg-white dark:bg-black text-black dark:text-white font-sans overflow-hidden transition-colors duration-300">
      {/* Sidebar */}
      <aside className="w-64 border-r border-zinc-200 dark:border-[#2C2C2E] flex flex-col hidden md:flex">
        <div className="p-6">
          <h1 className="text-2xl font-bold text-green-600 dark:text-[#00C805]">Stock MSA</h1>
          <p className="text-sm text-zinc-500 dark:text-[#8E8E93] mt-1">Quant Dashboard</p>
        </div>
        <nav className="flex-1 px-4 space-y-2">
          {navItems.map((item) => {
            const isActive = location.pathname === item.path;
            const Icon = item.icon;
            return (
              <Link
                key={item.path}
                to={item.path}
                className={`flex items-center gap-3 px-4 py-3 rounded-xl transition-colors ${
                  isActive
                    ? 'bg-zinc-100 dark:bg-[#111111] text-green-600 dark:text-[#00C805]'
                    : 'text-zinc-500 dark:text-[#8E8E93] hover:text-black dark:hover:text-white hover:bg-zinc-100 dark:hover:bg-[#111111]'
                }`}
              >
                <Icon size={20} />
                <span className="font-medium">{item.name}</span>
              </Link>
            );
          })}
        </nav>
      </aside>

      {/* Main Content */}
      <main className="flex-1 overflow-y-auto bg-zinc-50/50 dark:bg-black">
        {/* Header */}
        <header className="sticky top-0 z-30 flex items-center justify-between p-4 md:px-8 border-b border-zinc-200/50 dark:border-zinc-800/50 bg-white/80 dark:bg-black/80 backdrop-blur-md">
          <div className="md:hidden">
             <h1 className="text-xl font-bold text-green-600 dark:text-[#00C805]">Stock MSA</h1>
          </div>
          <StockSearch />
          <nav className="flex gap-4 md:hidden">
            {navItems.map((item) => {
              const Icon = item.icon;
              return (
                <Link
                  key={item.path}
                  to={item.path}
                  className={`${
                    location.pathname === item.path ? 'text-green-600 dark:text-[#00C805]' : 'text-zinc-500 dark:text-[#8E8E93]'
                  }`}
                >
                  <Icon size={24} />
                </Link>
              );
            })}
          </nav>
        </header>

        <div className="p-4 md:p-8 max-w-7xl mx-auto">
          {children}
        </div>
      </main>
    </div>
  );
};

export default function App() {
  return (
    <Layout>
      <Routes>
        <Route path="/" element={<DashboardHome />} />
        <Route path="/simulator" element={<BacktestSimulator />} />
        <Route path="/compare" element={<StrategyCompare />} />
        <Route path="/settings" element={<Settings />} />
        <Route path="/stock/:symbol" element={<StockDetail />} />
      </Routes>
    </Layout>
  );
}
