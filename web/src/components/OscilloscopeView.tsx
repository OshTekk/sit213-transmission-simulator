import React, { useState, useMemo } from 'react';
import {
  SimulationConfig,
  SimulationResult,
  WaveformType,
  EchoPath,
} from '../engine/dsp';
import {
  Activity,
  Cpu,
  Radio,
  ShieldCheck,
  AlertTriangle,
  Copy,
  Check,
  Plus,
  Trash2,
  Sliders,
  Eye,
  Zap,
  RefreshCw,
} from 'lucide-react';

interface OscilloscopeViewProps {
  config: SimulationConfig;
  setConfig: React.Dispatch<React.SetStateAction<SimulationConfig>>;
  result: SimulationResult;
}

export const OscilloscopeView: React.FC<OscilloscopeViewProps> = ({
  config,
  setConfig,
  result,
}) => {
  const [startChannelBit, setStartChannelBit] = useState(0);
  const [visibleChannelBits, setVisibleChannelBits] = useState(12);
  const [copiedCli, setCopiedCli] = useState(false);

  const totalChannelBits = result.codedBits.length;
  const maxStartBit = Math.max(0, totalChannelBits - visibleChannelBits);
  const clampedStartBit = Math.min(startChannelBit, maxStartBit);
  const endChannelBit = Math.min(totalChannelBits, clampedStartBit + visibleChannelBits);

  const handleCopyCli = () => {
    navigator.clipboard.writeText(result.cliCommand);
    setCopiedCli(true);
    setTimeout(() => setCopiedCli(false), 2000);
  };

  const setForm = (form: WaveformType) => {
    setConfig((prev) => ({
      ...prev,
      analogMode: true,
      form,
      amplMin: form === 'RZ' ? 0 : prev.amplMin === 0 ? -1 : prev.amplMin,
    }));
  };

  const addEcho = () => {
    if (config.echoes.length >= 5) return;
    setConfig((prev) => ({
      ...prev,
      echoes: [
        ...prev.echoes,
        {
          id: Math.random().toString(36).slice(2, 8),
          dt: prev.nbEch,
          ar: 0.5,
        },
      ],
    }));
  };

  const updateEcho = (id: string, field: 'dt' | 'ar', value: number) => {
    setConfig((prev) => ({
      ...prev,
      echoes: prev.echoes.map((e) => (e.id === id ? { ...e, [field]: value } : e)),
    }));
  };

  const removeEcho = (id: string) => {
    setConfig((prev) => ({
      ...prev,
      echoes: prev.echoes.filter((e) => e.id !== id),
    }));
  };

  // Construction des coordonnées SVG pour les signaux analogiques sur la fenêtre [clampedStartBit, endChannelBit]
  const svgWaveforms = useMemo(() => {
    const nbEch = result.config.analogMode ? result.config.nbEch : 1;
    const sStart = clampedStartBit * nbEch;
    const sEnd = endChannelBit * nbEch;
    const nSamples = Math.max(1, sEnd - sStart);

    // Calcul de l'échelle verticale dynamique
    let yMin = Math.min(result.config.amplMin, -0.5);
    let yMax = Math.max(result.config.amplMax, 1.5);
    for (let i = sStart; i < sEnd; i++) {
      if (result.rxSignal[i] < yMin) yMin = result.rxSignal[i];
      if (result.rxSignal[i] > yMax) yMax = result.rxSignal[i];
    }
    const pad = Math.max(0.4, 0.15 * (yMax - yMin));
    const vMin = yMin - pad;
    const vMax = yMax + pad;

    const width = 900;
    const height = 170;

    const toX = (sampleIdx: number) => ((sampleIdx - sStart) / nSamples) * width;
    const toY = (val: number) => height - ((val - vMin) / (vMax - vMin)) * height;

    const buildPath = (arr: Float32Array) => {
      const pts: string[] = [];
      for (let i = sStart; i < sEnd; i++) {
        const x1 = toX(i);
        const x2 = toX(i + 1);
        const y = toY(arr[i]);
        if (i === sStart) {
          pts.push(`M ${x1.toFixed(1)} ${y.toFixed(1)}`);
        } else {
          pts.push(`L ${x1.toFixed(1)} ${y.toFixed(1)}`);
        }
        pts.push(`L ${x2.toFixed(1)} ${y.toFixed(1)}`);
      }
      return pts.join(' ');
    };

    return {
      width,
      height,
      sStart,
      sEnd,
      nSamples,
      vMin,
      vMax,
      toX,
      toY,
      txPath: buildPath(result.txSignal),
      echoPath: buildPath(result.echoOnlySignal),
      rxPath: buildPath(result.rxSignal),
      zeroY: toY(0),
      thresholdY: toY(result.threshold),
    };
  }, [result, clampedStartBit, endChannelBit]);

  // Données du diagramme de l'œil (superposition de 40 traces de 2 périodes symbole)
  const eyeDiagramTraces = useMemo(() => {
    if (!result.config.analogMode) return null;
    const nbEch = result.config.nbEch;
    const span = 2 * nbEch;
    const maxTraces = Math.min(45, Math.floor(result.rxSignal.length / nbEch) - 2);
    if (maxTraces <= 0) return null;

    const width = 420;
    const height = 200;
    const vMin = result.config.amplMin < 0 ? -2.4 : -0.8;
    const vMax = 2.4;

    const toX = (k: number) => (k / span) * width;
    const toY = (v: number) => height - ((v - vMin) / (vMax - vMin)) * height;

    const cleanPaths: string[] = [];
    const noisyPaths: string[] = [];

    for (let t = 1; t <= maxTraces; t++) {
      const base = t * nbEch;
      const ptsClean: string[] = [];
      const ptsNoisy: string[] = [];
      for (let k = 0; k <= span; k++) {
        const idx = Math.min(result.rxSignal.length - 1, base + k);
        const x = toX(k).toFixed(1);
        const yc = toY(result.echoOnlySignal[idx]).toFixed(1);
        const yn = toY(result.rxSignal[idx]).toFixed(1);
        ptsClean.push(`${k === 0 ? 'M' : 'L'} ${x} ${yc}`);
        ptsNoisy.push(`${k === 0 ? 'M' : 'L'} ${x} ${yn}`);
      }
      cleanPaths.push(ptsClean.join(' '));
      noisyPaths.push(ptsNoisy.join(' '));
    }

    // Fenêtre d'intégration du symbole central [nbEch/2 .. 3*nbEch/2] ou [0..nbEch]
    const winStart =
      result.config.form === 'NRZ' ? 0 : result.nbEchLateral;
    const winEnd =
      result.config.form === 'NRZ'
        ? nbEch
        : result.nbEchLateral + result.nbEchMilieu;

    return {
      width,
      height,
      cleanPaths,
      noisyPaths,
      thresholdY: toY(result.threshold),
      zeroY: toY(0),
      winX1: toX(winStart),
      winX2: toX(winEnd),
      win2X1: toX(nbEch + winStart),
      win2X2: toX(nbEch + winEnd),
      midX: toX(nbEch),
    };
  }, [result]);

  return (
    <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
      {/* PANNEAU DE CONTRÔLE LATÉRAL (4 COLONNES) */}
      <div className="lg:col-span-4 space-y-4">
        {/* Presets rapides */}
        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4 shadow-lg">
          <div className="flex items-center justify-between mb-3">
            <h3 className="text-xs font-semibold uppercase tracking-wider text-cyan-400 flex items-center gap-1.5">
              <Zap className="w-3.5 h-3.5" /> Scénarios de Référence
            </h3>
            <span className="text-[11px] text-slate-400 font-mono">1-Click Load</span>
          </div>
          <div className="grid grid-cols-2 gap-2">
            <button
              onClick={() =>
                setConfig({
                  messageMode: 'fixed',
                  fixedMessage: '101100101101',
                  nbBits: 1000,
                  useSeed: true,
                  seed: 42,
                  analogMode: true,
                  form: 'NRZ',
                  nbEch: 30,
                  amplMin: -1,
                  amplMax: 1,
                  useSnr: true,
                  snrpbDb: 6.8,
                  echoes: [],
                  useCodeur: false,
                })
              }
              className="text-left px-3 py-2 rounded-lg bg-slate-800/80 hover:bg-cyan-950/60 border border-slate-700/70 hover:border-cyan-500/50 transition text-xs"
            >
              <div className="font-semibold text-slate-100">Env 1 · Optimal</div>
              <div className="text-[11px] text-slate-400">NRZ [-1,1] · 6.8 dB</div>
            </button>
            <button
              onClick={() =>
                setConfig({
                  messageMode: 'fixed',
                  fixedMessage: '10110010',
                  nbBits: 1000,
                  useSeed: true,
                  seed: 42,
                  analogMode: true,
                  form: 'NRZ',
                  nbEch: 30,
                  amplMin: -1,
                  amplMax: 1,
                  useSnr: true,
                  snrpbDb: 3.4,
                  echoes: [],
                  useCodeur: true,
                })
              }
              className="text-left px-3 py-2 rounded-lg bg-slate-800/80 hover:bg-cyan-950/60 border border-slate-700/70 hover:border-cyan-500/50 transition text-xs"
            >
              <div className="font-semibold text-slate-100">Env 1 · Codeur TP5</div>
              <div className="text-[11px] text-slate-400">Triplets 1/3 · 3.4 dB</div>
            </button>
            <button
              onClick={() =>
                setConfig({
                  messageMode: 'fixed',
                  fixedMessage: '1011001011001010',
                  nbBits: 1000,
                  useSeed: true,
                  seed: 42,
                  analogMode: true,
                  form: 'NRZ',
                  nbEch: 30,
                  amplMin: -1,
                  amplMax: 1,
                  useSnr: true,
                  snrpbDb: 15,
                  echoes: [{ id: 'e1', dt: 30, ar: 0.5 }],
                  useCodeur: false,
                })
              }
              className="text-left px-3 py-2 rounded-lg bg-slate-800/80 hover:bg-emerald-950/60 border border-slate-700/70 hover:border-emerald-500/50 transition text-xs"
            >
              <div className="font-semibold text-emerald-300">Env 2 · 100 kbit/s</div>
              <div className="text-[11px] text-slate-400">NRZ [-1,1] · dt=30, α=0.5</div>
            </button>
            <button
              onClick={() =>
                setConfig({
                  messageMode: 'fixed',
                  fixedMessage: '1011001011001010',
                  nbBits: 1000,
                  useSeed: true,
                  seed: 42,
                  analogMode: true,
                  form: 'NRZ',
                  nbEch: 30,
                  amplMin: 0,
                  amplMax: 1,
                  useSnr: true,
                  snrpbDb: 15,
                  echoes: [{ id: 'e1', dt: 30, ar: 0.5 }],
                  useCodeur: false,
                })
              }
              className="text-left px-3 py-2 rounded-lg bg-slate-800/80 hover:bg-rose-950/60 border border-slate-700/70 hover:border-rose-500/50 transition text-xs"
            >
              <div className="font-semibold text-rose-300">Env 2 · Œil Fermé</div>
              <div className="text-[11px] text-slate-400">NRZ [0,1] · dt=30, α=0.5</div>
            </button>
          </div>
        </div>

        {/* Source & Codage de canal */}
        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4 shadow-lg space-y-3">
          <h3 className="text-xs font-semibold uppercase tracking-wider text-slate-300 flex items-center gap-1.5">
            <Cpu className="w-3.5 h-3.5 text-cyan-400" /> 1. Source & Codage Canal (TP1 / TP5)
          </h3>

          <div className="grid grid-cols-2 gap-2 p-1 bg-slate-950 rounded-lg border border-slate-800">
            <button
              onClick={() => setConfig((p) => ({ ...p, messageMode: 'fixed' }))}
              className={`py-1.5 text-xs font-medium rounded-md transition ${
                config.messageMode === 'fixed'
                  ? 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/40'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              Message Fixe (-mess)
            </button>
            <button
              onClick={() => setConfig((p) => ({ ...p, messageMode: 'random' }))}
              className={`py-1.5 text-xs font-medium rounded-md transition ${
                config.messageMode === 'random'
                  ? 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/40'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              Aléatoire (-mess N)
            </button>
          </div>

          {config.messageMode === 'fixed' ? (
            <div>
              <label className="block text-xs text-slate-400 mb-1">
                Séquence binaire (&ge; 7 bits pour SourceFixe Java)
              </label>
              <input
                type="text"
                value={config.fixedMessage}
                onChange={(e) =>
                  setConfig((p) => ({
                    ...p,
                    fixedMessage: e.target.value.replace(/[^01]/g, ''),
                  }))
                }
                placeholder="10110010..."
                className="w-full px-3 py-1.5 bg-slate-950 border border-slate-700 rounded-lg font-mono text-sm text-cyan-300 focus:outline-none focus:border-cyan-500"
              />
            </div>
          ) : (
            <div>
              <div className="flex justify-between text-xs mb-1">
                <span className="text-slate-400">Longueur du message aléatoire</span>
                <span className="font-mono text-cyan-300 font-semibold">
                  {config.nbBits.toLocaleString()} bits
                </span>
              </div>
              <input
                type="range"
                min={20}
                max={20000}
                step={20}
                value={config.nbBits}
                onChange={(e) =>
                  setConfig((p) => ({ ...p, nbBits: Number(e.target.value) }))
                }
                className="w-full accent-cyan-400"
              />
              <div className="flex gap-1.5 mt-1.5">
                {[100, 1000, 5000, 20000].map((n) => (
                  <button
                    key={n}
                    onClick={() => setConfig((p) => ({ ...p, nbBits: n }))}
                    className="px-2 py-0.5 text-[11px] font-mono rounded bg-slate-800 hover:bg-slate-700 text-slate-300"
                  >
                    {n}
                  </button>
                ))}
              </div>
            </div>
          )}

          {/* Graine & Codeur */}
          <div className="flex items-center justify-between pt-2 border-t border-slate-800/80">
            <label className="flex items-center gap-2 text-xs text-slate-300 cursor-pointer">
              <input
                type="checkbox"
                checked={config.useSeed}
                onChange={(e) =>
                  setConfig((p) => ({ ...p, useSeed: e.target.checked }))
                }
                className="rounded accent-cyan-400"
              />
              Graine (-seed)
            </label>
            <div className="flex items-center gap-1.5">
              <input
                type="number"
                disabled={!config.useSeed}
                value={config.seed}
                onChange={(e) =>
                  setConfig((p) => ({ ...p, seed: Number(e.target.value) || 0 }))
                }
                className="w-20 px-2 py-1 bg-slate-950 border border-slate-700 rounded text-xs font-mono text-right text-slate-200 disabled:opacity-40"
              />
              <button
                onClick={() =>
                  setConfig((p) => ({
                    ...p,
                    useSeed: true,
                    seed: Math.floor(Math.random() * 9999),
                  }))
                }
                title="Nouvelle graine"
                className="p-1.5 rounded bg-slate-800 hover:bg-slate-700 text-slate-300"
              >
                <RefreshCw className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>

          <div className="flex items-center justify-between pt-2 border-t border-slate-800/80">
            <div>
              <div className="text-xs font-medium text-slate-200">
                Codeur Correcteur TP5 (-codeur)
              </div>
              <div className="text-[11px] text-slate-400 font-mono">
                0 &rarr; 010 · 1 &rarr; 101 (Rendement 1/3)
              </div>
            </div>
            <button
              onClick={() =>
                setConfig((p) => ({ ...p, useCodeur: !p.useCodeur }))
              }
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition ${
                config.useCodeur
                  ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/50'
                  : 'bg-slate-800 text-slate-400 border border-slate-700'
              }`}
            >
              {config.useCodeur ? 'ACTIF (×3 bits)' : 'DÉSACTIVÉ'}
            </button>
          </div>
        </div>

        {/* Mise en forme analogique (TP2) */}
        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4 shadow-lg space-y-3">
          <h3 className="text-xs font-semibold uppercase tracking-wider text-slate-300 flex items-center gap-1.5">
            <Sliders className="w-3.5 h-3.5 text-cyan-400" /> 2. Forme d'Onde & Échantillonnage (TP2)
          </h3>

          <div className="grid grid-cols-3 gap-2">
            {(['NRZ', 'NRZT', 'RZ'] as WaveformType[]).map((f) => (
              <button
                key={f}
                onClick={() => setForm(f)}
                className={`py-2 px-3 rounded-lg font-mono text-xs font-bold border transition ${
                  config.form === f
                    ? 'bg-cyan-500/20 border-cyan-400 text-cyan-300 shadow-sm'
                    : 'bg-slate-950 border-slate-800 text-slate-400 hover:text-slate-200'
                }`}
              >
                {f}
              </button>
            ))}
          </div>

          {/* Amplitudes */}
          <div>
            <label className="block text-xs text-slate-400 mb-1.5">
              Niveaux d'amplitude (-ampl min max)
            </label>
            <div className="grid grid-cols-2 gap-2">
              <button
                disabled={config.form === 'RZ'}
                onClick={() =>
                  setConfig((p) => ({ ...p, amplMin: -1.0, amplMax: 1.0 }))
                }
                className={`py-1.5 px-2 rounded-lg text-xs font-mono border transition ${
                  config.amplMin < 0
                    ? 'bg-indigo-500/20 border-indigo-400 text-indigo-200'
                    : 'bg-slate-950 border-slate-800 text-slate-400 disabled:opacity-40'
                }`}
              >
                Antipolaire [-1, +1]
              </button>
              <button
                onClick={() =>
                  setConfig((p) => ({ ...p, amplMin: 0.0, amplMax: 1.0 }))
                }
                className={`py-1.5 px-2 rounded-lg text-xs font-mono border transition ${
                  config.amplMin === 0
                    ? 'bg-amber-500/20 border-amber-400 text-amber-200'
                    : 'bg-slate-950 border-slate-800 text-slate-400'
                }`}
              >
                Unipolaire [0, +1]
              </button>
            </div>
            {config.form === 'RZ' && (
              <p className="text-[11px] text-amber-400/90 mt-1">
                Note : CodeRZ impose Amin = 0.0 conformément au contrat Java.
              </p>
            )}
          </div>

          {/* Échantillons par bit */}
          <div>
            <div className="flex justify-between text-xs mb-1">
              <span className="text-slate-400">Échantillons / bit (-nbEch)</span>
              <span className="font-mono text-cyan-300 font-semibold">
                {config.nbEch} éch/bit (zones: {result.nbEchLateral}/{result.nbEchMilieu}/{result.nbEchLateral})
              </span>
            </div>
            <input
              type="range"
              min={6}
              max={100}
              step={1}
              value={config.nbEch}
              onChange={(e) =>
                setConfig((p) => ({ ...p, nbEch: Number(e.target.value) }))
              }
              className="w-full accent-cyan-400"
            />
          </div>
        </div>

        {/* Canal Bruité & Multitrajets (TP3 / TP4) */}
        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4 shadow-lg space-y-3">
          <h3 className="text-xs font-semibold uppercase tracking-wider text-slate-300 flex items-center gap-1.5">
            <Radio className="w-3.5 h-3.5 text-amber-400" /> 3. Canal BBAG & Multitrajets (TP3 / TP4)
          </h3>

          <div className="flex items-center justify-between">
            <label className="flex items-center gap-2 text-xs text-slate-200 cursor-pointer">
              <input
                type="checkbox"
                checked={config.useSnr}
                onChange={(e) =>
                  setConfig((p) => ({ ...p, useSnr: e.target.checked }))
                }
                className="rounded accent-amber-400"
              />
              Activer Bruit Gaussien (-snrpb)
            </label>
            <span className="font-mono text-xs font-bold text-amber-300">
              {config.useSnr ? `${config.snrpbDb.toFixed(1)} dB` : 'Canal sans bruit'}
            </span>
          </div>

          {config.useSnr && (
            <div>
              <input
                type="range"
                min={-4}
                max={20}
                step={0.1}
                value={config.snrpbDb}
                onChange={(e) =>
                  setConfig((p) => ({ ...p, snrpbDb: Number(e.target.value) }))
                }
                className="w-full accent-amber-400"
              />
              <div className="flex justify-between text-[11px] font-mono text-slate-500">
                <span>-4 dB (Très bruité)</span>
                <span>6.8 dB (Seuil 10⁻³)</span>
                <span>15 dB (Env 2)</span>
              </div>
            </div>
          )}

          {/* Multitrajets */}
          <div className="pt-2 border-t border-slate-800/80 space-y-2">
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium text-slate-300">
                Trajets indirects (-ti dt ar) [{config.echoes.length}/5]
              </span>
              <button
                onClick={addEcho}
                disabled={config.echoes.length >= 5}
                className="flex items-center gap-1 px-2 py-1 rounded bg-cyan-500/20 hover:bg-cyan-500/30 text-cyan-300 border border-cyan-500/40 text-xs disabled:opacity-40"
              >
                <Plus className="w-3 h-3" /> Ajouter écho
              </button>
            </div>

            {config.echoes.length === 0 ? (
              <div className="text-xs text-slate-500 italic py-1">
                Aucun trajet réfléchi (canal à trajet direct unique α₀ = 1.0).
              </div>
            ) : (
              <div className="space-y-2">
                {config.echoes.map((echo, idx) => (
                  <div
                    key={echo.id}
                    className="p-2.5 rounded-lg bg-slate-950 border border-slate-800 space-y-1.5"
                  >
                    <div className="flex items-center justify-between text-xs">
                      <span className="font-mono text-cyan-300 font-semibold">
                        Écho #{idx + 1} · dt = {echo.dt} éch ({(echo.dt / config.nbEch).toFixed(2)} Ts)
                      </span>
                      <button
                        onClick={() => removeEcho(echo.id)}
                        className="text-slate-500 hover:text-rose-400"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                    <div className="grid grid-cols-2 gap-2 items-center">
                      <div>
                        <span className="text-[10px] text-slate-400 block">
                          Retard dt (0..{4 * config.nbEch})
                        </span>
                        <input
                          type="range"
                          min={0}
                          max={4 * config.nbEch}
                          step={1}
                          value={echo.dt}
                          onChange={(e) =>
                            updateEcho(echo.id, 'dt', Number(e.target.value))
                          }
                          className="w-full accent-cyan-400"
                        />
                      </div>
                      <div>
                        <span className="text-[10px] text-slate-400 block">
                          Amplitude α = {echo.ar.toFixed(2)}
                        </span>
                        <input
                          type="range"
                          min={0}
                          max={1}
                          step={0.05}
                          value={echo.ar}
                          onChange={(e) =>
                            updateEcho(echo.id, 'ar', Number(e.target.value))
                          }
                          className="w-full accent-emerald-400"
                        />
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* OSCILLOSCOPE & SONDES SYNCHRONISÉES (8 COLONNES) */}
      <div className="lg:col-span-8 space-y-4">
        {/* Barre de commande CLI & Métriques clés */}
        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4 shadow-lg space-y-3">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div className="flex items-center gap-2">
              <span className="px-2 py-0.5 text-[11px] font-mono uppercase rounded bg-cyan-500/15 text-cyan-300 border border-cyan-500/30">
                Commande Unique Java
              </span>
              <code className="text-xs font-mono text-slate-200 bg-slate-950 px-3 py-1.5 rounded-lg border border-slate-800 select-all">
                {result.cliCommand}
              </code>
            </div>
            <button
              onClick={handleCopyCli}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-xs font-medium text-slate-200 border border-slate-700 transition"
            >
              {copiedCli ? (
                <>
                  <Check className="w-3.5 h-3.5 text-emerald-400" /> Copié !
                </>
              ) : (
                <>
                  <Copy className="w-3.5 h-3.5" /> Copier CLI
                </>
              )}
            </button>
          </div>

          {/* KPIs Temps Réel */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 pt-2">
            <div className="p-3 rounded-lg bg-slate-950 border border-slate-800/90">
              <div className="text-[11px] text-slate-400">TEB Final (Utile)</div>
              <div
                className={`text-lg font-mono font-bold mt-0.5 ${
                  result.usefulTeb === 0
                    ? 'text-emerald-400'
                    : result.usefulTeb <= 1e-3
                    ? 'text-cyan-400'
                    : result.usefulTeb <= 1e-2
                    ? 'text-amber-400'
                    : 'text-rose-400'
                }`}
              >
                {result.usefulTeb === 0
                  ? '0.00000'
                  : result.usefulTeb.toExponential(3)}
              </div>
              <div className="text-[11px] text-slate-500 font-mono">
                {result.usefulErrorCount} / {result.sourceBits.length} bits erronés
              </div>
            </div>

            <div className="p-3 rounded-lg bg-slate-950 border border-slate-800/90">
              <div className="text-[11px] text-slate-400">TEB Brut Canal</div>
              <div className="text-lg font-mono font-bold text-slate-200 mt-0.5">
                {result.channelBer === 0
                  ? '0.00000'
                  : result.channelBer.toExponential(3)}
              </div>
              <div className="text-[11px] text-slate-500 font-mono">
                {result.channelErrorCount} / {result.codedBits.length} bits canal
              </div>
            </div>

            <div className="p-3 rounded-lg bg-slate-950 border border-slate-800/90">
              <div className="text-[11px] text-slate-400">Puissance & Bruit</div>
              <div className="text-sm font-mono font-bold text-amber-300 mt-0.5">
                Ps = {result.signalPower.toFixed(3)} W
              </div>
              <div className="text-[11px] text-slate-500 font-mono">
                σ²_b = {result.theoNoiseVariance.toFixed(3)} (emp: {result.empNoiseVariance.toFixed(3)})
              </div>
            </div>

            <div className="p-3 rounded-lg bg-slate-950 border border-slate-800/90">
              <div className="text-[11px] text-slate-400">Correction TP5</div>
              <div className="text-sm font-mono font-bold text-emerald-400 mt-0.5">
                {config.useCodeur
                  ? `${result.correctedTripletsCount} triplets sauvés`
                  : 'Codeur inactif'}
              </div>
              <div className="text-[11px] text-slate-500 font-mono">
                Seuil θ = {result.threshold.toFixed(2)}
              </div>
            </div>
          </div>
        </div>

        {/* CONTRÔLE DE FENÊTRE TEMPORELLE */}
        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4 shadow-lg space-y-4">
          <div className="flex flex-wrap items-center justify-between gap-4 pb-2 border-b border-slate-800">
            <div className="flex items-center gap-2">
              <Activity className="w-4 h-4 text-cyan-400" />
              <h3 className="text-sm font-semibold text-slate-100">
                Sondes Synchronisées (Bits canal #{clampedStartBit} à #{endChannelBit - 1})
              </h3>
            </div>
            <div className="flex items-center gap-4 text-xs">
              <div className="flex items-center gap-2">
                <span className="text-slate-400">Position :</span>
                <input
                  type="range"
                  min={0}
                  max={maxStartBit}
                  value={clampedStartBit}
                  onChange={(e) => setStartChannelBit(Number(e.target.value))}
                  className="w-28 accent-cyan-400"
                />
              </div>
              <div className="flex items-center gap-1.5">
                <span className="text-slate-400">Zoom :</span>
                {[6, 12, 18, 24].map((w) => (
                  <button
                    key={w}
                    onClick={() => setVisibleChannelBits(w)}
                    className={`px-2 py-0.5 rounded font-mono text-xs ${
                      visibleChannelBits === w
                        ? 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/40'
                        : 'bg-slate-800 text-slate-400'
                    }`}
                  >
                    {w}b
                  </button>
                ))}
              </div>
            </div>
          </div>

          {/* SONDE 1 : BITS ÉMIS SUR LE CANAL */}
          <div>
            <div className="flex justify-between text-xs mb-1">
              <span className="font-mono text-cyan-300 font-semibold">
                {config.useCodeur
                  ? 'Sonde Logique Codeur (Triplets émis 010 / 101)'
                  : 'Sonde Logique Source (Bits émis)'}
              </span>
              <span className="text-[11px] text-slate-400 font-mono">
                Information&lt;Boolean&gt;
              </span>
            </div>
            <div
              className="grid gap-1"
              style={{
                gridTemplateColumns: `repeat(${Math.max(1, endChannelBit - clampedStartBit)}, minmax(0, 1fr))`,
              }}
            >
              {result.codedBits
                .slice(clampedStartBit, endChannelBit)
                .map((bit, idx) => {
                  const globalIdx = clampedStartBit + idx;
                  const isTripletStart = config.useCodeur && globalIdx % 3 === 0;
                  return (
                    <div
                      key={globalIdx}
                      className={`h-8 rounded flex flex-col items-center justify-center font-mono text-xs font-bold border ${
                        bit
                          ? 'bg-cyan-500/20 border-cyan-500/50 text-cyan-200'
                          : 'bg-slate-950 border-slate-800 text-slate-400'
                      } ${isTripletStart ? 'ring-1 ring-indigo-400/50' : ''}`}
                    >
                      <span>{bit ? '1' : '0'}</span>
                      <span className="text-[9px] text-slate-500 font-normal">
                        #{globalIdx}
                      </span>
                    </div>
                  );
                })}
            </div>
          </div>

          {/* SONDE 2 : SIGNAL ANALOGIQUE ÉMETTEUR s(t) */}
          <div>
            <div className="flex justify-between text-xs mb-1">
              <span className="font-mono text-emerald-300 font-semibold">
                Sonde Analogique Émetteur s(t) — Code{config.form} ({config.nbEch} éch/bit)
              </span>
              <span className="text-[11px] text-slate-400 font-mono">
                Zones colorées = Fenêtre d'intégration du Récepteur
              </span>
            </div>
            <div className="bg-slate-950 border border-slate-800 rounded-lg p-2 overflow-hidden">
              <svg
                viewBox={`0 0 ${svgWaveforms.width} ${svgWaveforms.height}`}
                className="w-full h-36"
              >
                {/* Fenêtres d'intégration par bit */}
                {result.symbolDecisions
                  .slice(clampedStartBit, endChannelBit)
                  .map((sym) => {
                    const x1 = svgWaveforms.toX(sym.decisionStartSample);
                    const x2 = svgWaveforms.toX(sym.decisionEndSample);
                    const bx1 = svgWaveforms.toX(sym.startSample);
                    return (
                      <g key={sym.channelBitIndex}>
                        <rect
                          x={x1}
                          y={0}
                          width={Math.max(1, x2 - x1)}
                          height={svgWaveforms.height}
                          fill="rgba(16, 185, 129, 0.08)"
                        />
                        <line
                          x1={bx1}
                          y1={0}
                          x2={bx1}
                          y2={svgWaveforms.height}
                          stroke="#1e293b"
                          strokeDasharray="3 3"
                        />
                      </g>
                    );
                  })}

                {/* Ligne zéro et seuil */}
                <line
                  x1={0}
                  y1={svgWaveforms.zeroY}
                  x2={svgWaveforms.width}
                  y2={svgWaveforms.zeroY}
                  stroke="#334155"
                  strokeWidth={1}
                />
                <line
                  x1={0}
                  y1={svgWaveforms.thresholdY}
                  x2={svgWaveforms.width}
                  y2={svgWaveforms.thresholdY}
                  stroke="#f43f5e"
                  strokeDasharray="4 4"
                  strokeWidth={1.2}
                />

                {/* Trace s(t) */}
                <path
                  d={svgWaveforms.txPath}
                  fill="none"
                  stroke="#10b981"
                  strokeWidth={2.2}
                />
              </svg>
            </div>
          </div>

          {/* SONDE 3 : SIGNAL CANAL BRUITÉ + MULTITRAJETS r(t) */}
          <div>
            <div className="flex flex-wrap justify-between text-xs mb-1 gap-2">
              <span className="font-mono text-amber-300 font-semibold">
                Sonde Analogique Canal r(t) = s(t) + Σ αᵢ s(t - dtᵢ) + b(t)
              </span>
              <div className="flex items-center gap-3 text-[11px] font-mono">
                <span className="flex items-center gap-1 text-amber-300">
                  <span className="w-2.5 h-0.5 bg-amber-400 inline-block" /> r(t) total
                </span>
                <span className="flex items-center gap-1 text-cyan-300">
                  <span className="w-2.5 h-0.5 bg-cyan-400 inline-block" /> s(t)+échos (sans bruit)
                </span>
                <span className="flex items-center gap-1 text-rose-400">
                  <span className="w-2.5 h-0.5 bg-rose-500 inline-block" /> Seuil θ = {result.threshold}
                </span>
              </div>
            </div>
            <div className="bg-slate-950 border border-slate-800 rounded-lg p-2 overflow-hidden">
              <svg
                viewBox={`0 0 ${svgWaveforms.width} ${svgWaveforms.height}`}
                className="w-full h-40"
              >
                {/* Marquage des bits en erreur dans le canal */}
                {result.symbolDecisions
                  .slice(clampedStartBit, endChannelBit)
                  .map((sym) => {
                    const bx1 = svgWaveforms.toX(sym.startSample);
                    const bx2 = svgWaveforms.toX(sym.endSample);
                    const my = svgWaveforms.toY(sym.integratedMean);
                    return (
                      <g key={sym.channelBitIndex}>
                        {sym.isChannelError && (
                          <rect
                            x={bx1}
                            y={0}
                            width={Math.max(1, bx2 - bx1)}
                            height={svgWaveforms.height}
                            fill="rgba(244, 63, 94, 0.16)"
                          />
                        )}
                        <line
                          x1={bx1}
                          y1={0}
                          x2={bx1}
                          y2={svgWaveforms.height}
                          stroke="#1e293b"
                          strokeDasharray="3 3"
                        />
                        {/* Point de la moyenne intégrée sur la fenêtre de décision */}
                        <circle
                          cx={0.5 * (bx1 + bx2)}
                          cy={my}
                          r={3.5}
                          fill={sym.isChannelError ? '#f43f5e' : '#38bdf8'}
                          stroke="#090d16"
                          strokeWidth={1}
                        />
                      </g>
                    );
                  })}

                <line
                  x1={0}
                  y1={svgWaveforms.thresholdY}
                  x2={svgWaveforms.width}
                  y2={svgWaveforms.thresholdY}
                  stroke="#f43f5e"
                  strokeDasharray="5 4"
                  strokeWidth={1.5}
                />

                {/* Trace bruitée r(t) */}
                <path
                  d={svgWaveforms.rxPath}
                  fill="none"
                  stroke="#f59e0b"
                  strokeWidth={1.3}
                  opacity={0.85}
                />

                {/* Trace direct + échos sans bruit */}
                <path
                  d={svgWaveforms.echoPath}
                  fill="none"
                  stroke="#38bdf8"
                  strokeWidth={2}
                />
              </svg>
            </div>
          </div>

          {/* SONDE 4 : BITS DÉCODÉS PAR LE RÉCEPTEUR & MOYENNE INTÉGRÉE */}
          <div>
            <div className="flex justify-between text-xs mb-1">
              <span className="font-mono text-indigo-300 font-semibold">
                Sonde Logique Récepteur (Décision par moyenne &gt; θ = {result.threshold})
              </span>
              <span className="text-[11px] text-slate-400 font-mono">
                Rouge = Bit canal erroné
              </span>
            </div>
            <div
              className="grid gap-1"
              style={{
                gridTemplateColumns: `repeat(${Math.max(1, endChannelBit - clampedStartBit)}, minmax(0, 1fr))`,
              }}
            >
              {result.symbolDecisions
                .slice(clampedStartBit, endChannelBit)
                .map((sym) => (
                  <div
                    key={sym.channelBitIndex}
                    className={`py-1.5 px-1 rounded flex flex-col items-center justify-center font-mono text-xs border ${
                      sym.isChannelError
                        ? 'bg-rose-500/25 border-rose-500 text-rose-200'
                        : sym.rxBit
                        ? 'bg-indigo-500/20 border-indigo-500/40 text-indigo-200'
                        : 'bg-slate-950 border-slate-800 text-slate-300'
                    }`}
                  >
                    <span className="font-bold">{sym.rxBit ? '1' : '0'}</span>
                    <span className="text-[9px] text-slate-400">
                      {sym.integratedMean.toFixed(2)}
                    </span>
                  </div>
                ))}
            </div>
          </div>

          {/* SONDE 5 : DÉCODEUR DE CANAL TP5 (SI ACTIF) */}
          {config.useCodeur && result.tripletDecisions.length > 0 && (
            <div className="pt-2 border-t border-slate-800">
              <div className="flex justify-between text-xs mb-1.5">
                <span className="font-mono text-emerald-300 font-semibold flex items-center gap-1.5">
                  <ShieldCheck className="w-3.5 h-3.5" /> Sonde Décodeur TP5 — Vote majoritaire sur (a, ¬b, c)
                </span>
                <span className="text-[11px] font-mono text-slate-400">
                  Vert = Erreur canal corrigée (1/3) · Rouge = Triplet mis en échec (&ge;2/3)
                </span>
              </div>
              <div className="grid grid-cols-2 sm:grid-cols-4 md:grid-cols-6 gap-2">
                {result.tripletDecisions
                  .slice(
                    Math.floor(clampedStartBit / 3),
                    Math.ceil(endChannelBit / 3)
                  )
                  .map((tr) => (
                    <div
                      key={tr.usefulBitIndex}
                      className={`p-2 rounded-lg border font-mono text-xs ${
                        tr.isUsefulError
                          ? 'bg-rose-950/60 border-rose-500/70 text-rose-200'
                          : tr.wasCorrected
                          ? 'bg-emerald-950/60 border-emerald-500/70 text-emerald-200'
                          : 'bg-slate-950 border-slate-800 text-slate-300'
                      }`}
                    >
                      <div className="flex justify-between text-[10px] text-slate-400 mb-0.5">
                        <span>Bit #{tr.usefulBitIndex}</span>
                        <span>
                          {tr.isUsefulError
                            ? 'ÉCHEC'
                            : tr.wasCorrected
                            ? 'CORRIGÉ'
                            : 'OK'}
                        </span>
                      </div>
                      <div className="text-center font-bold text-sm">
                        {tr.rxTriplet.map((b) => (b ? '1' : '0')).join('')} &rarr;{' '}
                        <span className="underline">
                          {tr.rxUsefulBit ? '1' : '0'}
                        </span>
                      </div>
                    </div>
                  ))}
              </div>
            </div>
          )}
        </div>

        {/* DIAGRAMME DE L'ŒIL & ANALYSE DE LA MARGE DE DÉCISION */}
        {eyeDiagramTraces && (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4 shadow-lg">
              <div className="flex items-center justify-between mb-2">
                <h4 className="text-xs font-semibold uppercase tracking-wider text-cyan-400 flex items-center gap-1.5">
                  <Eye className="w-3.5 h-3.5" /> Diagramme de l'Œil (2 Périodes Ts)
                </h4>
                <span className="text-[11px] font-mono text-slate-400">
                  45 traces superposées
                </span>
              </div>
              <div className="bg-slate-950 border border-slate-800 rounded-lg p-2">
                <svg
                  viewBox={`0 0 ${eyeDiagramTraces.width} ${eyeDiagramTraces.height}`}
                  className="w-full h-44"
                >
                  {/* Fenêtres de décision des 2 symboles */}
                  <rect
                    x={eyeDiagramTraces.winX1}
                    y={0}
                    width={eyeDiagramTraces.winX2 - eyeDiagramTraces.winX1}
                    height={eyeDiagramTraces.height}
                    fill="rgba(16, 185, 129, 0.08)"
                  />
                  <rect
                    x={eyeDiagramTraces.win2X1}
                    y={0}
                    width={eyeDiagramTraces.win2X2 - eyeDiagramTraces.win2X1}
                    height={eyeDiagramTraces.height}
                    fill="rgba(16, 185, 129, 0.08)"
                  />
                  <line
                    x1={eyeDiagramTraces.midX}
                    y1={0}
                    x2={eyeDiagramTraces.midX}
                    y2={eyeDiagramTraces.height}
                    stroke="#334155"
                    strokeDasharray="3 3"
                  />
                  <line
                    x1={0}
                    y1={eyeDiagramTraces.thresholdY}
                    x2={eyeDiagramTraces.width}
                    y2={eyeDiagramTraces.thresholdY}
                    stroke="#f43f5e"
                    strokeDasharray="4 4"
                    strokeWidth={1.5}
                  />

                  {/* Traces bruitées en arrière-plan */}
                  {eyeDiagramTraces.noisyPaths.map((d, i) => (
                    <path
                      key={`n-${i}`}
                      d={d}
                      fill="none"
                      stroke="#f59e0b"
                      strokeWidth={0.9}
                      opacity={0.22}
                    />
                  ))}

                  {/* Traces direct + écho (sans bruit) */}
                  {eyeDiagramTraces.cleanPaths.map((d, i) => (
                    <path
                      key={`c-${i}`}
                      d={d}
                      fill="none"
                      stroke="#38bdf8"
                      strokeWidth={1.6}
                      opacity={0.65}
                    />
                  ))}
                </svg>
              </div>
              <p className="text-[11px] text-slate-400 mt-2">
                En cyan : trajectoires déterministes (signal direct + échos). En ambre : réalisations bruitées. La ligne rouge pointillée indique le seuil de décision θ = {result.threshold}.
              </p>
            </div>

            {/* Nuage des moyennes intégrées par symbole */}
            <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4 shadow-lg flex flex-col justify-between">
              <div>
                <h4 className="text-xs font-semibold uppercase tracking-wider text-emerald-400 mb-2">
                  Répartition des Moyennes Intégrées ȳₖ vs Seuil θ
                </h4>
                <div className="bg-slate-950 border border-slate-800 rounded-lg p-3 space-y-3">
                  {['Bit 1 émis (Attendu > θ)', 'Bit 0 émis (Attendu ≤ θ)'].map(
                    (label, idx) => {
                      const targetBit = idx === 0;
                      const subset = result.symbolDecisions.filter(
                        (s) => s.txBit === targetBit
                      );
                      const means = subset.map((s) => s.integratedMean);
                      const minM =
                        means.length > 0 ? Math.min(...means) : 0;
                      const maxM =
                        means.length > 0 ? Math.max(...means) : 0;
                      const avgM =
                        means.length > 0
                          ? means.reduce((a, b) => a + b, 0) / means.length
                          : 0;
                      const errs = subset.filter((s) => s.isChannelError).length;

                      return (
                        <div
                          key={label}
                          className="p-2.5 rounded bg-slate-900/90 border border-slate-800"
                        >
                          <div className="flex justify-between text-xs mb-1">
                            <span
                              className={`font-semibold ${
                                targetBit ? 'text-cyan-300' : 'text-amber-300'
                              }`}
                            >
                              {label}
                            </span>
                            <span className="font-mono text-[11px] text-slate-400">
                              Erreurs : {errs} / {subset.length}
                            </span>
                          </div>
                          <div className="grid grid-cols-3 gap-2 text-xs font-mono">
                            <div>
                              <span className="text-slate-500 text-[10px] block">
                                Moyenne
                              </span>
                              <span className="text-slate-200">
                                {avgM.toFixed(3)}
                              </span>
                            </div>
                            <div>
                              <span className="text-slate-500 text-[10px] block">
                                Min observé
                              </span>
                              <span className="text-slate-200">
                                {minM.toFixed(3)}
                              </span>
                            </div>
                            <div>
                              <span className="text-slate-500 text-[10px] block">
                                Max observé
                              </span>
                              <span className="text-slate-200">
                                {maxM.toFixed(3)}
                              </span>
                            </div>
                          </div>
                        </div>
                      );
                    }
                  )}
                </div>
              </div>

              <div className="mt-3 p-2.5 rounded-lg bg-cyan-950/30 border border-cyan-500/30 text-xs text-cyan-200 flex items-start gap-2">
                <AlertTriangle className="w-4 h-4 text-cyan-400 shrink-0 mt-0.5" />
                <div>
                  <strong>Règle de décision Java :</strong>{' '}
                  {config.form === 'NRZ'
                    ? `CodeNRZ intègre les ${config.nbEch} échantillons complets du symbole (filtre adapté rectangulaire).`
                    : `Code${config.form} ignore les ${result.nbEchLateral} échantillons latéraux à gauche et à droite, et moyenne uniquement les ${result.nbEchMilieu} échantillons du tiers central.`}
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
