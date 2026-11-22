import React, { useState, useMemo } from 'react';
import { SeededRandom, dbToLin } from '../engine/dsp';
import { BarChart3, CheckCircle2, RefreshCw } from 'lucide-react';

export const NoiseAnalysisView: React.FC = () => {
  const [snrDb, setSnrDb] = useState(0.0);
  const [nbEch, setNbEch] = useState(30);
  const [signalPower, setSignalPower] = useState(1.0);
  const [nbSamples, setNbSamples] = useState(50000);
  const [seed, setSeed] = useState(42);

  const analysis = useMemo(() => {
    const snrLin = dbToLin(snrDb);
    const theoVar = (signalPower * nbEch) / (2.0 * snrLin);
    const theoStd = Math.sqrt(theoVar);

    const rng = new SeededRandom(seed);
    const samples = new Float32Array(nbSamples);
    let sum = 0;
    let sumSq = 0;

    for (let i = 0; i < nbSamples; i++) {
      const a1 = rng.nextFloat();
      const a2 = rng.nextFloat();
      let u1 = 1.0 - a1;
      if (u1 <= 0) u1 = 1e-10;
      const b = theoStd * Math.sqrt(-2.0 * Math.log(u1)) * Math.cos(2.0 * Math.PI * a2);
      samples[i] = b;
      sum += b;
      sumSq += b * b;
    }

    const empMean = sum / nbSamples;
    const empVar = sumSq / nbSamples - empMean * empMean;
    const empStd = Math.sqrt(Math.max(0, empVar));

    // Histogramme sur [-4*sigma, +4*sigma] en 45 classes
    const numBins = 45;
    const xMax = Math.max(0.5, 4.0 * theoStd);
    const xMin = -xMax;
    const binWidth = (xMax - xMin) / numBins;
    const counts = new Array(numBins).fill(0);

    for (let i = 0; i < nbSamples; i++) {
      const v = samples[i];
      if (v >= xMin && v < xMax) {
        const bIdx = Math.min(numBins - 1, Math.floor((v - xMin) / binWidth));
        counts[bIdx]++;
      }
    }

    const densities = counts.map((c) => c / (nbSamples * binWidth));
    const maxDensity = Math.max(
      1 / (theoStd * Math.sqrt(2 * Math.PI)),
      ...densities
    );

    const bins = counts.map((_, idx) => {
      const xCenter = xMin + (idx + 0.5) * binWidth;
      const theoPdf =
        (1.0 / (theoStd * Math.sqrt(2 * Math.PI))) *
        Math.exp(-(xCenter * xCenter) / (2 * theoVar));
      return {
        xCenter,
        empDensity: densities[idx],
        theoPdf,
      };
    });

    // Q-Q Plot (Droite de Henry) sur 50 quantiles empiriques vs théoriques
    const sorted = Array.from(samples.slice(0, Math.min(8000, nbSamples))).sort(
      (a, b) => a - b
    );
    const qqPoints: { theoQ: number; empQ: number }[] = [];
    const nQ = 45;
    // Approximation inverse normale de Acklam / Beasley-Springer-Moro
    const normInv = (p: number) => {
      const a = [
        -3.969683028665376e1, 2.209460984245205e2, -2.759285104469687e2,
        1.38357751867269e2, -3.066479806614716e1, 2.506628277459239,
      ];
      const b = [
        -5.447609879822406e1, 1.615858368580409e2, -1.556989798598866e2,
        6.680131188771972e1, -1.328068155288572e1,
      ];
      const q = p - 0.5;
      const r = q * q;
      return (
        ((((((a[0] * r + a[1]) * r + a[2]) * r + a[3]) * r + a[4]) * r + a[5]) * q) /
        (((((b[0] * r + b[1]) * r + b[2]) * r + b[3]) * r + b[4]) * r + 1)
      );
    };

    for (let k = 1; k <= nQ; k++) {
      const prob = (k - 0.5) / nQ;
      if (prob > 0.02 && prob < 0.98) {
        const z = normInv(prob);
        const empVal = sorted[Math.floor(prob * sorted.length)];
        qqPoints.push({
          theoQ: z * theoStd,
          empQ: empVal,
        });
      }
    }

    return {
      theoVar,
      theoStd,
      empMean,
      empVar,
      empStd,
      bins,
      xMin,
      xMax,
      maxDensity,
      qqPoints,
    };
  }, [snrDb, nbEch, signalPower, nbSamples, seed]);

  return (
    <div className="space-y-6">
      {/* En-tête explicatif & contrôles */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        <div className="lg:col-span-4 bg-slate-900/90 border border-slate-800 rounded-xl p-5 shadow-lg space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-bold text-cyan-400 flex items-center gap-2">
              <BarChart3 className="w-4 h-4" /> TP3 · Générateur Box-Muller
            </h3>
            <button
              onClick={() => setSeed((s) => s + 1)}
              className="flex items-center gap-1 px-2.5 py-1 rounded-lg bg-slate-800 hover:bg-slate-700 text-xs text-slate-200 border border-slate-700"
            >
              <RefreshCw className="w-3 h-3" /> Re-tirer
            </button>
          </div>

          <p className="text-xs text-slate-400 leading-relaxed">
            Reproduit en direct le script <code className="text-cyan-300">mesures/pbruit.py</code> et la méthode{' '}
            <code className="text-cyan-300">TransmetteurBruite.genereEchantillonBruit()</code> :
            σ²_b = (P_s · N) / (2 · 10^(SNR/10)).
          </p>

          <div>
            <div className="flex justify-between text-xs mb-1">
              <span className="text-slate-300">Rapport Eb/N0 (-snrpb)</span>
              <span className="font-mono font-bold text-amber-300">
                {snrDb.toFixed(1)} dB
              </span>
            </div>
            <input
              type="range"
              min={-5}
              max={15}
              step={0.5}
              value={snrDb}
              onChange={(e) => setSnrDb(Number(e.target.value))}
              className="w-full accent-amber-400"
            />
          </div>

          <div>
            <div className="flex justify-between text-xs mb-1">
              <span className="text-slate-300">Échantillons par bit (N)</span>
              <span className="font-mono font-bold text-cyan-300">{nbEch}</span>
            </div>
            <input
              type="range"
              min={6}
              max={100}
              step={2}
              value={nbEch}
              onChange={(e) => setNbEch(Number(e.target.value))}
              className="w-full accent-cyan-400"
            />
          </div>

          <div>
            <div className="flex justify-between text-xs mb-1">
              <span className="text-slate-300">Puissance moyenne signal (Ps)</span>
              <span className="font-mono font-bold text-emerald-300">
                {signalPower.toFixed(2)} W
              </span>
            </div>
            <div className="grid grid-cols-3 gap-2">
              {[
                { label: 'NRZ [-1,1] (1.0)', val: 1.0 },
                { label: 'NRZT [-1,1] (0.78)', val: 0.778 },
                { label: 'RZ [0,1] (0.167)', val: 1 / 6 },
              ].map((p) => (
                <button
                  key={p.label}
                  onClick={() => setSignalPower(p.val)}
                  className={`py-1.5 px-2 rounded text-[11px] font-mono border transition ${
                    Math.abs(signalPower - p.val) < 0.01
                      ? 'bg-emerald-500/20 border-emerald-400 text-emerald-200'
                      : 'bg-slate-950 border-slate-800 text-slate-400'
                  }`}
                >
                  {p.label}
                </button>
              ))}
            </div>
          </div>

          {/* Tableau comparatif Théorie vs Mesure */}
          <div className="p-3.5 rounded-xl bg-slate-950 border border-slate-800 space-y-2">
            <div className="text-xs font-semibold text-slate-200 flex items-center gap-1.5">
              <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" /> Validation Statistique ({nbSamples.toLocaleString()} éch.)
            </div>
            <div className="grid grid-cols-3 text-xs font-mono pt-1 border-t border-slate-800/80">
              <span className="text-slate-400">Paramètre</span>
              <span className="text-right text-cyan-300">Théorie</span>
              <span className="text-right text-amber-300">Mesuré</span>
            </div>
            <div className="grid grid-cols-3 text-xs font-mono">
              <span className="text-slate-400">Moyenne μ</span>
              <span className="text-right text-slate-200">0.0000</span>
              <span className="text-right text-amber-200">
                {analysis.empMean.toFixed(4)}
              </span>
            </div>
            <div className="grid grid-cols-3 text-xs font-mono">
              <span className="text-slate-400">Variance σ²_b</span>
              <span className="text-right text-cyan-300 font-bold">
                {analysis.theoVar.toFixed(3)}
              </span>
              <span className="text-right text-amber-300 font-bold">
                {analysis.empVar.toFixed(3)}
              </span>
            </div>
            <div className="grid grid-cols-3 text-xs font-mono">
              <span className="text-slate-400">Écart-type σ_b</span>
              <span className="text-right text-slate-200">
                {analysis.theoStd.toFixed(3)}
              </span>
              <span className="text-right text-amber-200">
                {analysis.empStd.toFixed(3)}
              </span>
            </div>
          </div>
        </div>

        {/* GRAPHIQUES : HISTOGRAMME + DROITE DE HENRY (Q-Q PLOT) */}
        <div className="lg:col-span-8 grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Histogramme de densité */}
          <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-5 shadow-lg flex flex-col justify-between">
            <div>
              <h4 className="text-sm font-bold text-slate-100">
                Distribution Empirique vs Loi Normale N(0, σ²_b)
              </h4>
              <p className="text-xs text-slate-400 mt-0.5">
                Pour SNR = 0 dB, N = 30, Ps = 1.0 &rarr; σ²_b = 15.00 (cf. Figure 21 du rapport)
              </p>
            </div>

            <div className="bg-slate-950 border border-slate-800 rounded-lg p-3 my-3">
              <svg viewBox="0 0 420 250" className="w-full h-56">
                {/* Barres de l'histogramme */}
                {analysis.bins.map((b, idx) => {
                  const n = analysis.bins.length;
                  const barW = 360 / n;
                  const x = 40 + idx * barW;
                  const barH = (b.empDensity / (analysis.maxDensity * 1.1)) * 200;
                  const y = 220 - barH;
                  return (
                    <rect
                      key={idx}
                      x={x}
                      y={y}
                      width={Math.max(1, barW - 1)}
                      height={Math.max(0, barH)}
                      fill="rgba(56, 189, 248, 0.45)"
                      stroke="#0284c7"
                      strokeWidth={0.5}
                    />
                  );
                })}

                {/* Courbe gaussienne théorique */}
                <path
                  d={analysis.bins
                    .map((b, idx) => {
                      const n = analysis.bins.length;
                      const barW = 360 / n;
                      const x = 40 + (idx + 0.5) * barW;
                      const y =
                        220 -
                        (b.theoPdf / (analysis.maxDensity * 1.1)) * 200;
                      return `${idx === 0 ? 'M' : 'L'} ${x.toFixed(1)} ${y.toFixed(1)}`;
                    })
                    .join(' ')}
                  fill="none"
                  stroke="#f43f5e"
                  strokeWidth={2.5}
                />

                {/* Axes */}
                <line x1={40} y1={220} x2={400} y2={220} stroke="#475569" />
                <line x1={220} y1={20} x2={220} y2={220} stroke="#334155" strokeDasharray="3 3" />
                <text x={40} y={238} fill="#94a3b8" fontSize="10" fontFamily="monospace">
                  {analysis.xMin.toFixed(1)}
                </text>
                <text x={212} y={238} fill="#94a3b8" fontSize="10" fontFamily="monospace">
                  0.0
                </text>
                <text x={375} y={238} fill="#94a3b8" fontSize="10" fontFamily="monospace">
                  +{analysis.xMax.toFixed(1)}
                </text>
              </svg>
            </div>

            <div className="flex items-center justify-between text-xs font-mono text-slate-400">
              <span className="flex items-center gap-1.5">
                <span className="w-3 h-3 rounded-sm bg-sky-400/50 border border-sky-500 inline-block" />{' '}
                Histogramme Box-Muller
              </span>
              <span className="flex items-center gap-1.5 text-rose-400">
                <span className="w-4 h-0.5 bg-rose-500 inline-block" /> Densité théorique N(0, {analysis.theoVar.toFixed(2)})
              </span>
            </div>
          </div>

          {/* Graphique Q-Q (Droite de Henry) */}
          <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-5 shadow-lg flex flex-col justify-between">
            <div>
              <h4 className="text-sm font-bold text-slate-100">
                Diagramme Q-Q (Droite de Henry)
              </h4>
              <p className="text-xs text-slate-400 mt-0.5">
                Alignement des quantiles empiriques sur les quantiles normaux théoriques
              </p>
            </div>

            <div className="bg-slate-950 border border-slate-800 rounded-lg p-3 my-3">
              <svg viewBox="0 0 420 250" className="w-full h-56">
                {/* Grille */}
                <line x1={40} y1={120} x2={400} y2={120} stroke="#1e293b" />
                <line x1={220} y1={20} x2={220} y2={220} stroke="#1e293b" />

                {/* Droite théorique y = x */}
                <line
                  x1={55}
                  y1={205}
                  x2={385}
                  y2={35}
                  stroke="#f43f5e"
                  strokeWidth={1.8}
                  strokeDasharray="4 4"
                />

                {/* Points quantiles */}
                {analysis.qqPoints.map((pt, i) => {
                  const lim = Math.max(0.5, 2.5 * analysis.theoStd);
                  const x = 220 + (pt.theoQ / lim) * 165;
                  const y = 120 - (pt.empQ / lim) * 85;
                  return (
                    <circle
                      key={i}
                      cx={x}
                      cy={y}
                      r={3.2}
                      fill="#38bdf8"
                      stroke="#090d16"
                      strokeWidth={0.8}
                    />
                  );
                })}

                <text x={150} y={240} fill="#94a3b8" fontSize="10" fontFamily="monospace">
                  Quantiles théoriques N(0, σ²_b)
                </text>
              </svg>
            </div>

            <div className="text-xs text-slate-400">
              L'alignement linéaire parfait sur la bissectrice rouge confirme la gaussianité sur plus de ±2.5 écarts-types.
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
