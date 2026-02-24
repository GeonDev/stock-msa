import { Routes, Route, Link, useLocation } from 'react-router-dom';
import { Home, LineChart, LayoutDashboard, Settings } from 'lucide-react';
import DashboardHome from './pages/DashboardHome';
import BacktestSimulator from './pages/BacktestSimulator';
import StrategyCompare from './pages/StrategyCompare';

const Layout = ({ children }: { children: React.ReactNode }) => {
  const location = useLocation();

  const navItems = [
    { path: '/', name: 'Home', icon: Home },
    { path: '/simulator', name: 'Simulator', icon: LineChart },
    { path: '/compare', name: 'Compare', icon: LayoutDashboard },
  ];

  return (
    <div className="flex h-screen bg-black text-white font-sans overflow-hidden">
      {/* Sidebar */}
      <aside className="w-64 border-r border-[#2C2C2E] flex flex-col hidden md:flex">
        <div className="p-6">
          <h1 className="text-2xl font-bold text-[#00C805]">Stock MSA</h1>
          <p className="text-sm text-[#8E8E93] mt-1">Quant Dashboard</p>
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
                    ? 'bg-[#111111] text-[#00C805]'
                    : 'text-[#8E8E93] hover:text-white hover:bg-[#111111]'
                }`}
              >
                <Icon size={20} />
                <span className="font-medium">{item.name}</span>
              </Link>
            );
          })}
        </nav>
        <div className="p-6 border-t border-[#2C2C2E]">
          <button className="flex items-center gap-3 text-[#8E8E93] hover:text-white transition-colors">
            <Settings size={20} />
            <span className="font-medium">Settings</span>
          </button>
        </div>
      </aside>

      {/* Main Content */}
      <main className="flex-1 overflow-y-auto">
        {/* Mobile Header */}
        <header className="md:hidden p-4 border-b border-[#2C2C2E] flex items-center justify-between">
          <h1 className="text-xl font-bold text-[#00C805]">Stock MSA</h1>
          <nav className="flex gap-4">
            {navItems.map((item) => {
              const Icon = item.icon;
              return (
                <Link
                  key={item.path}
                  to={item.path}
                  className={`${
                    location.pathname === item.path ? 'text-[#00C805]' : 'text-[#8E8E93]'
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
      </Routes>
    </Layout>
  );
}
