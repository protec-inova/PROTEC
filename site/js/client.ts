/**
 * Cliente da API PROTEC (REST).
 * Sem dependências: usa fetch (navegador ou Node 18+). ES Module.
 *
 * Usage:
 *   import { createProtecClient } from "./protec-client";
 *   const api = createProtecClient({ baseUrl: "http://localhost:8081" });
 *   const maquinas = await api.maquinas.listAll();
 */

// ── Tipos ───────────────────────────────────────────────────────────────────

export interface Maquina {
  id: string;
  nome: string;
  riscos: string;
}

export interface Funcionario {
  id: string;
  nome: string;
}

export interface Consentimento {
  id: number;
  funcionarioId: string;
  maquinaId: string;
  /** CA do óculos */
  oculos: string | null;
  /** CA da bota */
  bota: string | null;
  /** Momento do aceite (ISO 8601) */
  data: string;
  /** Fim da validade (ISO 8601) */
  expiraEm: string;
  valido: boolean;
  segundosRestantes: number;
}

export interface HealthDependency {
  name: string;
  status: string;
  latency: number;
}

export interface Health {
  status: string;
  service: string;
  version: string;
  timestamp: string;
  javaVersion: string;
  springBootVersion: string;
  environment: string;
  dependencies: HealthDependency[];
}

export interface ClientOptions {
  /** Padrão: http://localhost:8081 */
  baseUrl?: string;
  /** Padrão: 10000 */
  timeoutMs?: number;
  /** Implementação de fetch (testes ou Node antigo) */
  fetch?: typeof fetch;
}

export interface RequestOptions {
  signal?: AbortSignal;
}

export interface PageParams extends RequestOptions {
  /** Começa em 0 */
  page?: number;
  /** Máximo 100 no servidor */
  size?: number;
}

export interface CreateMaquinaInput {
  nome: string;
  riscos: string;
}

export interface CreateFuncionarioInput {
  nome: string;
}

export interface CreateConsentimentoInput {
  funcionarioId: string;
  maquinaId: string;
  /** CA do óculos (até 20 caracteres) */
  oculos?: string;
  /** CA da bota (até 20 caracteres) */
  bota?: string;
}

/** Exatamente um dos dois: funcionarioId OU maquinaId. */
export type UltimoFiltro =
  | { funcionarioId: string; maquinaId?: never }
  | { maquinaId: string; funcionarioId?: never };

// ── Erro ────────────────────────────────────────────────────────────────────

interface ProtecErrorDetails {
  status?: number;
  url?: string;
  body?: unknown;
  cause?: unknown;
}

const STATUS_MESSAGES: Record<number, string> = {
  400: "Requisição inválida",
  404: "Não encontrado",
  409: "Conflito",
  500: "Erro interno do servidor",
};

export class ProtecError extends Error {
  /** 0 = falha de rede ou timeout */
  readonly status: number;
  readonly url: string | null;
  readonly body: unknown;

  constructor(message: string, details: ProtecErrorDetails = {}) {
    if (details.cause !== undefined) {
      super(message, { cause: details.cause });
    } else {
      super(message);
    }
    this.name = "ProtecError";
    this.status = details.status ?? 0;
    this.url = details.url ?? null;
    this.body = details.body ?? null;
  }

  get isNetworkError(): boolean {
    return this.status === 0;
  }

  get isBadRequest(): boolean {
    return this.status === 400;
  }

  get isNotFound(): boolean {
    return this.status === 404;
  }

  get isConflict(): boolean {
    return this.status === 409;
  }
}

// ── Utilitários ─────────────────────────────────────────────────────────────

type QueryValue = string | number | boolean | null | undefined;
type Query = Record<string, QueryValue>;

const DEFAULT_BASE_URL = "http://localhost:8081";
const DEFAULT_TIMEOUT_MS = 10000;

function buildQuery(params?: Query): string {
  if (!params) {
    return "";
  }

  const search = new URLSearchParams();

  for (const key of Object.keys(params)) {
    const value = params[key];
    if (value === undefined || value === null) {
      continue;
    }
    search.append(key, String(value));
  }

  const text = search.toString();
  if (text === "") {
    return "";
  }
  return "?" + text;
}

function segment(value: string | number): string {
  return encodeURIComponent(String(value));
}

// ── Cliente ─────────────────────────────────────────────────────────────────

export function createProtecClient(options: ClientOptions = {}) {
  const baseUrl = (options.baseUrl ?? DEFAULT_BASE_URL).replace(/\/+$/, "");
  const timeoutMs = options.timeoutMs ?? DEFAULT_TIMEOUT_MS;
  const fetchImpl = options.fetch ?? globalThis.fetch;

  if (typeof fetchImpl !== "function") {
    throw new Error("fetch não disponível: informe options.fetch");
  }

  async function request<T>(
    method: string,
    path: string,
    query?: Query,
    signal?: AbortSignal,
  ): Promise<T> {
    const url = baseUrl + path + buildQuery(query);

    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), timeoutMs);

    if (signal) {
      if (signal.aborted) {
        controller.abort();
      } else {
        signal.addEventListener("abort", () => controller.abort(), { once: true });
      }
    }

    let response: Response;
    try {
      response = await fetchImpl(url, {
        method,
        headers: { Accept: "application/json" },
        signal: controller.signal,
      });
    } catch (cause) {
      const cancelledByCaller = signal !== undefined && signal.aborted;
      const timedOut = controller.signal.aborted && !cancelledByCaller;
      const message = timedOut
        ? "Tempo esgotado ao chamar " + url
        : "Falha de rede ao chamar " + url;
      throw new ProtecError(message, { url, cause });
    } finally {
      clearTimeout(timer);
    }

    const text = await response.text();
    let body: unknown = null;
    if (text !== "") {
      try {
        body = JSON.parse(text);
      } catch {
        body = text;
      }
    }

    if (!response.ok) {
      const message = STATUS_MESSAGES[response.status] ?? "Erro HTTP " + response.status;
      throw new ProtecError(message, { status: response.status, url, body });
    }

    return body as T;
  }

  // ── Máquinas ────────────────────────────────────────────────────────────

  const maquinas = {
    listAll(opts: RequestOptions = {}): Promise<Maquina[]> {
      return request("GET", "/maquinas", undefined, opts.signal);
    },

    getById(id: string, opts: RequestOptions = {}): Promise<Maquina> {
      return request("GET", "/maquinas/" + segment(id), undefined, opts.signal);
    },

    getByNome(nome: string, opts: RequestOptions = {}): Promise<Maquina> {
      return request("GET", "/maquinas/nome/" + segment(nome), undefined, opts.signal);
    },

    create({ nome, riscos }: CreateMaquinaInput, opts: RequestOptions = {}): Promise<Maquina> {
      return request("POST", "/maquinas", { nome, riscos }, opts.signal);
    },

    updateNome(id: string, nome: string, opts: RequestOptions = {}): Promise<Maquina> {
      return request("PATCH", "/maquinas/" + segment(id) + "/nome", { nome }, opts.signal);
    },

    updateRiscos(id: string, riscos: string, opts: RequestOptions = {}): Promise<Maquina> {
      return request("PATCH", "/maquinas/" + segment(id) + "/riscos", { riscos }, opts.signal);
    },

    updateNomeByNome(nome: string, newNome: string, opts: RequestOptions = {}): Promise<Maquina> {
      return request("PATCH", "/maquinas/nome/" + segment(nome) + "/nome", { newNome }, opts.signal);
    },

    updateRiscosByNome(nome: string, riscos: string, opts: RequestOptions = {}): Promise<Maquina> {
      return request("PATCH", "/maquinas/nome/" + segment(nome) + "/riscos", { riscos }, opts.signal);
    },

    delete(id: string, opts: RequestOptions = {}): Promise<null> {
      return request("DELETE", "/maquinas/" + segment(id), undefined, opts.signal);
    },

    deleteByNome(nome: string, opts: RequestOptions = {}): Promise<null> {
      return request("DELETE", "/maquinas/nome/" + segment(nome), undefined, opts.signal);
    },
  };

  // ── Funcionários ────────────────────────────────────────────────────────

  const funcionarios = {
    listAll(opts: RequestOptions = {}): Promise<Funcionario[]> {
      return request("GET", "/funcionarios", undefined, opts.signal);
    },

    getById(id: string, opts: RequestOptions = {}): Promise<Funcionario> {
      return request("GET", "/funcionarios/" + segment(id), undefined, opts.signal);
    },

    getByNome(nome: string, opts: RequestOptions = {}): Promise<Funcionario> {
      return request("GET", "/funcionarios/nome/" + segment(nome), undefined, opts.signal);
    },

    create({ nome }: CreateFuncionarioInput, opts: RequestOptions = {}): Promise<Funcionario> {
      return request("POST", "/funcionarios", { nome }, opts.signal);
    },

    updateNome(id: string, nome: string, opts: RequestOptions = {}): Promise<Funcionario> {
      return request("PATCH", "/funcionarios/" + segment(id) + "/nome", { nome }, opts.signal);
    },

    updateNomeByNome(nome: string, newNome: string, opts: RequestOptions = {}): Promise<Funcionario> {
      return request("PATCH", "/funcionarios/nome/" + segment(nome) + "/nome", { newNome }, opts.signal);
    },

    delete(id: string, opts: RequestOptions = {}): Promise<null> {
      return request("DELETE", "/funcionarios/" + segment(id), undefined, opts.signal);
    },

    deleteByNome(nome: string, opts: RequestOptions = {}): Promise<null> {
      return request("DELETE", "/funcionarios/nome/" + segment(nome), undefined, opts.signal);
    },
  };

  // ── Consentimentos ──────────────────────────────────────────────────────

  const consentimentos = {
    listAll(opts: RequestOptions = {}): Promise<Consentimento[]> {
      return request("GET", "/consentimentos", undefined, opts.signal);
    },

    getById(id: number, opts: RequestOptions = {}): Promise<Consentimento> {
      return request("GET", "/consentimentos/" + segment(id), undefined, opts.signal);
    },

    /**
     * Último aceite por funcionário OU por máquina (exatamente um dos dois).
     * Por máquina, é o último aceite de qualquer funcionário.
     */
    getUltimo(filtro: UltimoFiltro, opts: RequestOptions = {}): Promise<Consentimento> {
      const { funcionarioId, maquinaId } = filtro;
      const temFuncionario = funcionarioId !== undefined && funcionarioId !== null;
      const temMaquina = maquinaId !== undefined && maquinaId !== null;

      if (temFuncionario === temMaquina) {
        throw new TypeError("getUltimo: informe funcionarioId OU maquinaId (apenas um)");
      }

      return request("GET", "/consentimentos/ultimo", { funcionarioId, maquinaId }, opts.signal);
    },

    getUltimoByFuncionario(funcionarioId: string, opts: RequestOptions = {}): Promise<Consentimento> {
      return consentimentos.getUltimo({ funcionarioId }, opts);
    },

    getUltimoByMaquina(maquinaId: string, opts: RequestOptions = {}): Promise<Consentimento> {
      return consentimentos.getUltimo({ maquinaId }, opts);
    },

    /** Histórico do funcionário, mais recente primeiro. */
    listByFuncionario(funcionarioId: string, params: PageParams = {}): Promise<Consentimento[]> {
      const { page, size, signal } = params;
      return request(
        "GET",
        "/consentimentos/funcionario/" + segment(funcionarioId),
        { page, size },
        signal,
      );
    },

    /** Histórico da máquina, mais recente primeiro. */
    listByMaquina(maquinaId: string, params: PageParams = {}): Promise<Consentimento[]> {
      const { page, size, signal } = params;
      return request(
        "GET",
        "/consentimentos/maquina/" + segment(maquinaId),
        { page, size },
        signal,
      );
    },

    /** Registra o aceite do termo. oculos/bota = CA de cada EPI (opcionais). */
    create(
      { funcionarioId, maquinaId, oculos, bota }: CreateConsentimentoInput,
      opts: RequestOptions = {},
    ): Promise<Consentimento> {
      return request("POST", "/consentimentos", { funcionarioId, maquinaId, oculos, bota }, opts.signal);
    },
  };

  // ── Health ──────────────────────────────────────────────────────────────

  const health = {
    get(opts: RequestOptions = {}): Promise<Health> {
      return request("GET", "/health", undefined, opts.signal);
    },
  };

  return { maquinas, funcionarios, consentimentos, health };
}

export type ProtecClient = ReturnType<typeof createProtecClient>;

export default createProtecClient;