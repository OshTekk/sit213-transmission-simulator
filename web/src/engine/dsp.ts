/**
 * SIT213 — Moteur DSP & Simulation Numérique (Portage fidèle bit-à-bit de l'architecture Java)
 * Reproduit exactement : SourceFixe, SourceAleatoire, Codeur (TP5), CodeNRZ, CodeRZ, CodeNRZT,
 * TransmetteurBruite (Box-Muller + Multitrajets TP3/TP4), Recepteur (TP2), Decodeur (TP5).
 */

export type WaveformType = 'NRZ' | 'NRZT' | 'RZ';

export interface EchoPath {
  id: string;
  dt: number;       // Retard en échantillons (entier >= 0)
  ar: number;       // Amplitude relative dans [0.0, 1.0]
}

export interface SimulationConfig {
  messageMode: 'random' | 'fixed';
  nbBits: number;          // Nombre de bits utiles si random (1..100000)
  fixedMessage: string;    // Chaîne binaire >= 7 bits si fixed
  useSeed: boolean;
  seed: number;
  analogMode: boolean;
  form: WaveformType;
  nbEch: number;           // Échantillons par bit de canal (défaut 30)
  amplMin: number;         // Défaut -1.0 (NRZ/NRZT) ou 0.0 (RZ)
  amplMax: number;         // Défaut +1.0
  useSnr: boolean;
  snrpbDb: number;         // Eb/N0 par bit canal en dB (-snrpb)
  echoes: EchoPath[];      // Jusqu'à 5 trajets indirects (-ti dt ar)
  useCodeur: boolean;      // Codage de canal en triplets 0->010, 1->101 (-codeur)
}

export interface SymbolDecisionDetail {
  channelBitIndex: number;
  txBit: boolean;
  rxBit: boolean;
  isChannelError: boolean;
  startSample: number;
  endSample: number;
  decisionStartSample: number;
  decisionEndSample: number;
  integratedMean: number;
  cleanMean: number;
  threshold: number;
}

export interface TripletDecisionDetail {
  usefulBitIndex: number;
  txUsefulBit: boolean;
  rxUsefulBit: boolean;
  txTriplet: [boolean, boolean, boolean];
  rxTriplet: [boolean, boolean, boolean];
  channelErrorsInTriplet: number;
  wasCorrected: boolean;
  isUsefulError: boolean;
}

export interface SimulationResult {
  config: SimulationConfig;
  sourceBits: boolean[];
  codedBits: boolean[];
  txSignal: Float32Array;
  echoOnlySignal: Float32Array;
  noiseSignal: Float32Array;
  rxSignal: Float32Array;
  rxChannelBits: boolean[];
  destinationBits: boolean[];
  symbolDecisions: SymbolDecisionDetail[];
  tripletDecisions: TripletDecisionDetail[];
  signalPower: number;
  theoNoiseVariance: number;
  empNoiseMean: number;
  empNoiseVariance: number;
  threshold: number;
  channelErrorCount: number;
  channelBer: number;
  usefulErrorCount: number;
  usefulTeb: number;
  correctedTripletsCount: number;
  nbEchLateral: number;
  nbEchMilieu: number;
  cliCommand: string;
}

/**
 * Générateur pseudo-aléatoire déterministe (Mulberry32) initialisé avec une graine entière
 */
export class SeededRandom {
  private state: number;

  constructor(seed?: number) {
    const s = seed !== undefined ? seed : Math.floor(Math.random() * 0x7fffffff);
    this.state = (s | 0) ^ 0x6d2b79f5;
  }

  nextFloat(): number {
    let t = (this.state += 0x6d2b79f5);
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  }

  nextBoolean(): boolean {
    return this.nextFloat() >= 0.5;
  }
}

/**
 * Approximation haute précision de erfc(x) (Abramowitz & Stegun 7.1.26, erreur < 1.5e-7)
 */
export function erfc(x: number): number {
  const z = Math.abs(x);
  const t = 1.0 / (1.0 + 0.5 * z);
  const ans =
    t *
    Math.exp(
      -z * z -
        1.26551223 +
        t *
          (1.00002368 +
            t *
              (0.37409196 +
                t *
                  (0.09678418 +
                    t *
                      (-0.18628806 +
                        t *
                          (0.27886807 +
                            t *
                              (-1.13520398 +
                                t * (1.48851587 + t * (-0.82215223 + t * 0.17087277))))))))
    );
  return x >= 0 ? ans : 2.0 - ans;
}

/**
 * Fonction Q(x) = P(N(0,1) > x) = 0.5 * erfc(x / sqrt(2))
 */
export function Q(x: number): number {
  return 0.5 * erfc(x / Math.SQRT2);
}

export function dbToLin(db: number): number {
  return Math.pow(10, db / 10);
}

export function linToDb(lin: number): number {
  return 10 * Math.log10(lin);
}

/**
 * Résolution par dichotomie de f(x) = target pour f décroissante sur [a, b]
 */
export function dichotomie(f: (x: number) => number, target: number, a = 0.001, b = 200): number {
  let low = a;
  let high = b;
  for (let i = 0; i < 100; i++) {
    const mid = 0.5 * (low + high);
    if (f(mid) > target) {
      low = mid;
    } else {
      high = mid;
    }
  }
  return 0.5 * (low + high);
}

/**
 * Formules théoriques de TEB sur canal BBAG (sans multitrajet)
 */
export function tebTheoNrzAntipolaire(ebn0Lin: number): number {
  return Q(Math.sqrt(2 * ebn0Lin));
}

export function tebTheoUnipolaire(ebn0Lin: number): number {
  // Valable pour NRZ unipolaire [0, A] et RZ unipolaire [0, A] (décision sur tiers central)
  return Q(Math.sqrt(ebn0Lin));
}

export function tebTheoNrztAntipolaire(ebn0Lin: number, nbEch = 30): number {
  // Puissance moyenne discrète NRZT avec Ne=30 : Ps ≈ 0.778333 * A^2, intégration sur Ne/3
  const psNorm = 0.778333;
  const tiersRatio = Math.floor(nbEch / 3) / nbEch; // 1/3 pour Ne multiple de 3
  return Q(Math.sqrt((2 * ebn0Lin * tiersRatio) / psNorm));
}

export function tebTheoAvecCodeurUtile(ebn0UtileLin: number): number {
  // 1 bit utile = 3 bits canal -> Ec/N0 = (Eb_utile / N0) / 3
  const p = tebTheoNrzAntipolaire(ebn0UtileLin / 3);
  return 3 * p * p - 2 * p * p * p;
}

export function tebTheoAvecCodeurCanal(ecN0CanalLin: number): number {
  const p = tebTheoNrzAntipolaire(ecN0CanalLin);
  return 3 * p * p - 2 * p * p * p;
}

/**
 * Découpage exact d'un bit en 3 zones comme dans CodeRZ.java et CodeNRZT.java (TP5)
 */
export function getThirdsPartition(nbEchParBit: number): {
  nbEchLateral: number;
  nbEchMilieu: number;
} {
  const tiers = Math.floor(nbEchParBit / 3);
  const reste = nbEchParBit % 3;
  const nbEchLateral = tiers + (reste === 2 ? 1 : 0);
  const nbEchMilieu = tiers + (reste === 1 ? 1 : 0);
  return { nbEchLateral, nbEchMilieu };
}

/**
 * Génère la commande CLI Java exacte correspondant à la configuration
 */
export function buildJavaCliCommand(cfg: SimulationConfig): string {
  const args: string[] = ['./script/simulateur'];
  if (cfg.messageMode === 'fixed') {
    const clean = cfg.fixedMessage.replace(/[^01]/g, '') || '10110010';
    args.push('-mess', clean.length >= 7 ? clean : clean.padEnd(7, '0'));
  } else {
    args.push('-mess', String(Math.max(1, Math.min(999999, Math.round(cfg.nbBits)))));
  }

  if (cfg.useSeed) {
    args.push('-seed', String(Math.round(cfg.seed)));
  }

  if (cfg.analogMode) {
    args.push('-form', cfg.form);
    args.push('-nbEch', String(cfg.nbEch));
    const minVal = cfg.form === 'RZ' ? 0 : cfg.amplMin;
    args.push('-ampl', String(minVal), String(cfg.amplMax));

    if (cfg.useSnr) {
      args.push('-snrpb', String(Number(cfg.snrpbDb.toFixed(3))));
    }

    if (cfg.echoes.length > 0) {
      args.push('-ti');
      for (const e of cfg.echoes.slice(0, 5)) {
        args.push(String(Math.max(0, Math.round(e.dt))), String(Number(e.ar.toFixed(2))));
      }
    }
  }

  if (cfg.useCodeur) {
    args.push('-codeur');
  }

  return args.join(' ');
}

/**
 * Exécute une simulation complète de la chaîne SIT213
 */
export function runSimulation(rawConfig: SimulationConfig): SimulationResult {
  const cfg: SimulationConfig = {
    ...rawConfig,
    amplMin: rawConfig.form === 'RZ' ? 0 : rawConfig.amplMin,
    nbEch: Math.max(3, Math.round(rawConfig.nbEch)),
  };

  // 1. Source (SourceFixe ou SourceAleatoire)
  const sourceRng = new SeededRandom(cfg.useSeed ? cfg.seed : undefined);
  const sourceBits: boolean[] = [];
  if (cfg.messageMode === 'fixed') {
    const clean = cfg.fixedMessage.replace(/[^01]/g, '') || '10110010';
    for (let i = 0; i < clean.length; i++) {
      sourceBits.push(clean[i] === '1');
    }
  } else {
    const n = Math.max(1, Math.min(100000, Math.round(cfg.nbBits)));
    for (let i = 0; i < n; i++) {
      sourceBits.push(sourceRng.nextBoolean());
    }
  }

  // 2. Codeur de canal TP5 (0 -> 010, 1 -> 101)
  const codedBits: boolean[] = [];
  if (cfg.useCodeur) {
    for (let i = 0; i < sourceBits.length; i++) {
      const b = sourceBits[i];
      codedBits.push(b, !b, b);
    }
  } else {
    for (let i = 0; i < sourceBits.length; i++) {
      codedBits.push(sourceBits[i]);
    }
  }

  const nChannelBits = codedBits.length;
  const nbEch = cfg.nbEch;
  const totalSamples = cfg.analogMode ? nChannelBits * nbEch : nChannelBits;
  const { nbEchLateral, nbEchMilieu } = getThirdsPartition(nbEch);
  const threshold = 0.5 * (cfg.amplMin + cfg.amplMax);

  // 3. Émetteur analogique (CodeNRZ, CodeRZ, CodeNRZT)
  const txSignal = new Float32Array(totalSamples);
  if (!cfg.analogMode) {
    for (let i = 0; i < nChannelBits; i++) {
      txSignal[i] = codedBits[i] ? 1 : 0;
    }
  } else if (cfg.form === 'NRZ') {
    let idx = 0;
    for (let i = 0; i < nChannelBits; i++) {
      const val = codedBits[i] ? cfg.amplMax : cfg.amplMin;
      for (let k = 0; k < nbEch; k++) {
        txSignal[idx++] = val;
      }
    }
  } else if (cfg.form === 'RZ') {
    let idx = 0;
    for (let i = 0; i < nChannelBits; i++) {
      const midVal = codedBits[i] ? cfg.amplMax : cfg.amplMin;
      for (let k = 0; k < nbEchLateral; k++) txSignal[idx++] = cfg.amplMin;
      for (let k = 0; k < nbEchMilieu; k++) txSignal[idx++] = midVal;
      for (let k = 0; k < nbEchLateral; k++) txSignal[idx++] = cfg.amplMin;
    }
  } else {
    // NRZT (CodeNRZT.java)
    let idx = 0;
    const amplMoy = threshold;
    for (let i = 0; i < nChannelBits; i++) {
      const bit = codedBits[i];
      const amplPic = bit ? cfg.amplMax : cfg.amplMin;

      // Zone latérale gauche
      if (i === 0 || bit !== codedBits[i - 1]) {
        const pente = (amplPic - amplMoy) / nbEchLateral;
        for (let k = 0; k < nbEchLateral; k++) {
          txSignal[idx++] = amplMoy + k * pente;
        }
      } else {
        for (let k = 0; k < nbEchLateral; k++) {
          txSignal[idx++] = amplPic;
        }
      }

      // Plateau central
      for (let k = 0; k < nbEchMilieu; k++) {
        txSignal[idx++] = amplPic;
      }

      // Zone latérale droite
      if (i === nChannelBits - 1 || bit !== codedBits[i + 1]) {
        const pente = (amplMoy - amplPic) / nbEchLateral;
        for (let k = 0; k < nbEchLateral; k++) {
          txSignal[idx++] = amplPic + k * pente;
        }
      } else {
        for (let k = 0; k < nbEchLateral; k++) {
          txSignal[idx++] = amplPic;
        }
      }
    }
  }

  // 4. Canal (TransmetteurBruite.java : calcul puissance Ps, multitrajets et Box-Muller)
  let sumSq = 0;
  for (let i = 0; i < totalSamples; i++) {
    sumSq += txSignal[i] * txSignal[i];
  }
  const signalPower = totalSamples > 0 ? sumSq / totalSamples : 0;

  let theoNoiseVariance = 0;
  if (cfg.analogMode && cfg.useSnr) {
    const snrLin = dbToLin(cfg.snrpbDb);
    theoNoiseVariance = (signalPower * nbEch) / (2.0 * snrLin);
  }
  const noiseStd = Math.sqrt(Math.max(0, theoNoiseVariance));

  const echoOnlySignal = new Float32Array(totalSamples);
  const noiseSignal = new Float32Array(totalSamples);
  const rxSignal = new Float32Array(totalSamples);

  const channelRng = new SeededRandom(cfg.useSeed ? cfg.seed + 10007 : undefined);
  const activeEchoes = cfg.analogMode ? cfg.echoes.slice(0, 5) : [];

  let noiseSum = 0;
  let noiseSqSum = 0;

  for (let i = 0; i < totalSamples; i++) {
    let multi = 0;
    for (let m = 0; m < activeEchoes.length; m++) {
      const dt = Math.round(activeEchoes[m].dt);
      if (dt <= i) {
        multi += activeEchoes[m].ar * txSignal[i - dt];
      }
    }

    let b = 0;
    if (cfg.analogMode && cfg.useSnr && noiseStd > 0) {
      const a1 = channelRng.nextFloat();
      const a2 = channelRng.nextFloat();
      let u1 = 1.0 - a1;
      if (u1 <= 0.0) u1 = 1e-10;
      b = noiseStd * Math.sqrt(-2.0 * Math.log(u1)) * Math.cos(2.0 * Math.PI * a2);
    }

    noiseSum += b;
    noiseSqSum += b * b;
    echoOnlySignal[i] = txSignal[i] + multi;
    noiseSignal[i] = b;
    rxSignal[i] = txSignal[i] + multi + b;
  }

  const empNoiseMean = totalSamples > 0 ? noiseSum / totalSamples : 0;
  const empNoiseVariance =
    totalSamples > 0 ? noiseSqSum / totalSamples - empNoiseMean * empNoiseMean : 0;

  // 5. Récepteur analogique (CodeNRZ, CodeRZ, CodeNRZT decoder)
  const rxChannelBits: boolean[] = new Array(nChannelBits);
  const symbolDecisions: SymbolDecisionDetail[] = [];
  let channelErrorCount = 0;

  for (let i = 0; i < nChannelBits; i++) {
    const startSample = cfg.analogMode ? i * nbEch : i;
    const endSample = cfg.analogMode ? (i + 1) * nbEch : i + 1;

    let decisionStart = startSample;
    let decisionEnd = endSample;

    if (cfg.analogMode && (cfg.form === 'RZ' || cfg.form === 'NRZT')) {
      decisionStart = startSample + nbEchLateral;
      decisionEnd = decisionStart + nbEchMilieu;
    }

    let sumRx = 0;
    let sumClean = 0;
    const count = decisionEnd - decisionStart;
    for (let k = decisionStart; k < decisionEnd; k++) {
      sumRx += rxSignal[k];
      sumClean += echoOnlySignal[k];
    }
    const meanRx = count > 0 ? sumRx / count : 0;
    const meanClean = count > 0 ? sumClean / count : 0;

    const decodedBit = cfg.analogMode ? meanRx > threshold : rxSignal[i] > 0.5;
    rxChannelBits[i] = decodedBit;

    const isErr = decodedBit !== codedBits[i];
    if (isErr) channelErrorCount++;

    // Pour garder la mémoire fluide sur les très longs messages, on stocke le détail des 300 premiers symboles
    if (i < 300) {
      symbolDecisions.push({
        channelBitIndex: i,
        txBit: codedBits[i],
        rxBit: decodedBit,
        isChannelError: isErr,
        startSample,
        endSample,
        decisionStartSample: decisionStart,
        decisionEndSample: decisionEnd,
        integratedMean: meanRx,
        cleanMean: meanClean,
        threshold,
      });
    }
  }

  // 6. Décodeur de canal TP5 (Decodeur.java)
  const destinationBits: boolean[] = [];
  const tripletDecisions: TripletDecisionDetail[] = [];
  let usefulErrorCount = 0;
  let correctedTripletsCount = 0;

  if (cfg.useCodeur) {
    const nTriplets = Math.floor(nChannelBits / 3);
    for (let u = 0; u < nTriplets; u++) {
      const a = rxChannelBits[3 * u];
      const b = rxChannelBits[3 * u + 1];
      const c = rxChannelBits[3 * u + 2];

      const ta = codedBits[3 * u];
      const tb = codedBits[3 * u + 1];
      const tc = codedBits[3 * u + 2];

      const decodedUseful = (a && !b) || (a && c) || (!b && c);
      destinationBits.push(decodedUseful);

      const chErrors = (a !== ta ? 1 : 0) + (b !== tb ? 1 : 0) + (c !== tc ? 1 : 0);
      const isUsefulErr = decodedUseful !== sourceBits[u];
      const wasCorrected = chErrors === 1 && !isUsefulErr;

      if (wasCorrected) correctedTripletsCount++;
      if (isUsefulErr) usefulErrorCount++;

      if (u < 150) {
        tripletDecisions.push({
          usefulBitIndex: u,
          txUsefulBit: sourceBits[u],
          rxUsefulBit: decodedUseful,
          txTriplet: [ta, tb, tc],
          rxTriplet: [a, b, c],
          channelErrorsInTriplet: chErrors,
          wasCorrected,
          isUsefulError: isUsefulErr,
        });
      }
    }
  } else {
    for (let u = 0; u < nChannelBits; u++) {
      const b = rxChannelBits[u];
      destinationBits.push(b);
      if (b !== sourceBits[u]) usefulErrorCount++;
    }
  }

  const channelBer = nChannelBits > 0 ? channelErrorCount / nChannelBits : 0;
  const usefulTeb = sourceBits.length > 0 ? usefulErrorCount / sourceBits.length : 0;

  return {
    config: cfg,
    sourceBits,
    codedBits,
    txSignal,
    echoOnlySignal,
    noiseSignal,
    rxSignal,
    rxChannelBits,
    destinationBits,
    symbolDecisions,
    tripletDecisions,
    signalPower,
    theoNoiseVariance,
    empNoiseMean,
    empNoiseVariance,
    threshold,
    channelErrorCount,
    channelBer,
    usefulErrorCount,
    usefulTeb,
    correctedTripletsCount,
    nbEchLateral,
    nbEchMilieu,
    cliCommand: buildJavaCliCommand(cfg),
  };
}

/**
 * Résultats de référence pré-calculés avec le simulateur Java (100 000 bits, graine 42, Ne = 30)
 * issus de mesures/environnement1/resultats.json et mesures/environnement2/resultats_q4.json
 */
export const ENV1_REFERENCE_DATA = {
  snrs: [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10],
  curves: {
    'NRZ antipolaire [-1, 1]': [0.07878, 0.0563, 0.0383, 0.02306, 0.0124, 0.00612, 0.0027, 0.00087, 0.00024, 6e-5, 0],
    'NRZ unipolaire [0, 1]': [0.15831, 0.13059, 0.10434, 0.07935, 0.05674, 0.03863, 0.02348, 0.01257, 0.00622, 0.00273, 0.0009],
    'RZ [0, 1]': [0.1587, 0.13161, 0.10402, 0.07945, 0.05725, 0.03817, 0.02367, 0.01305, 0.00604, 0.00251, 0.00084],
    'NRZT antipolaire [-1, 1]': [0.17732, 0.14964, 0.12201, 0.09526, 0.07158, 0.05023, 0.03265, 0.01925, 0.01042, 0.00447, 0.00176],
    'NRZ + Codeur (Eb/N0 utile)': [0.1108, 0.08596, 0.06288, 0.04267, 0.02686, 0.01569, 0.00758, 0.00338, 0.00114, 0.00029, 7e-5],
  },
};

export const ENV2_REFERENCE_DATA = {
  debitsKbps: [10, 25, 50, 75, 100, 125, 150, 200, 250, 300, 400, 500],
  debitCurves15dB: {
    'NRZ antipolaire [-1, 1]': [0, 0, 0, 0, 3e-5, 1e-5, 1e-5, 1e-5, 1e-5, 3e-5, 3e-5, 3e-5],
    'NRZT antipolaire [-1, 1]': [0, 0, 0, 0.00043, 0.00254, 0.00134, 0.00126, 0.00231, 0.00116, 0.00226, 0.00239, 0.00229],
    'NRZ unipolaire [0, 1]': [0, 0, 0.00075, 0.02472, 0.12607, 0.07213, 0.06294, 0.12517, 0.06301, 0.12424, 0.12485, 0.12562],
  },
  snrsDb: [5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15],
  snrCurves: {
    'NRZ 100 kbit/s (dt=30)': [0.05375, 0.04001, 0.02863, 0.01925, 0.01167, 0.00639, 0.00317, 0.00138, 0.00044, 9e-5, 3e-5],
    'NRZ 200 kbit/s (dt=60)': [0.05363, 0.0394, 0.02833, 0.01927, 0.01162, 0.00633, 0.00311, 0.00132, 0.00039, 8e-5, 1e-5],
    'NRZT 100 kbit/s (dt=30)': [0.10485, 0.09049, 0.07564, 0.06184, 0.0482, 0.03636, 0.02583, 0.01729, 0.01028, 0.00547, 0.00254],
    'NRZT 200 kbit/s (dt=60)': [0.10567, 0.09102, 0.07609, 0.06207, 0.04846, 0.03648, 0.02567, 0.01658, 0.00991, 0.00534, 0.00231],
  },
};
