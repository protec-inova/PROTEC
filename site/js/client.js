/**
 * Cliente da API PROTEC (REST).
 * Sem dependências: usa fetch (navegador ou Node 18+). ES Module.
 *
 * Uso:
 *   import { createProtecClient } from "./protec-client.js";
 *   const api = createProtecClient({ baseUrl: "http://localhost:8081" });
 *   const maquinas = await api.maquinas.listAll();
 */

/**
 * @typedef {Object} Maquina
 * @property {string} id
 * @property {string} nome
 * @property {string} riscos
 */

/**
 * @typedef {Object} Funcionario
 * @property {string} id
 * @property {string} nome
 */

/**
 * @typedef {Object} Consentimento
 * @property {number} id
 * @property {string} funcionarioId
 * @property {string} maquinaId
 * @property {string|null} oculos CA do óculos
 * @property {string|null} bota CA da bota
 * @property {string} data momento do aceite (ISO 8601)
 * @property {string} expiraEm fim da validade (ISO 8601)
 * @property {boolean} valido
 * @property {number} segundosRestantes
 */

/**
 * @typedef {Object} Health
 * @property {string} status
 * @property {string} service
 * @property {string} version
 * @property {string} timestamp
 * @property {string} javaVersion
 * @property {string} springBootVersion
 * @property {string} environment
 * @property {{name: string, status: string, latency: number}[]} dependencies
 */

/**
 * @typedef {Object} ClientOptions
 * @property {string} [baseUrl] padrão: http://localhost:8081
 * @property {number} [timeoutMs] padrão: 10000
 * @property {typeof fetch} [fetch] implementação de fetch (testes ou Node antigo)
 */

const DEFAULT_BASE_URL = "http://localhost:8081";
const DEFAULT_TIMEOUT_MS = 10000;

const STATUS_MESSAGES = {
  400: "Requisição inválida",
  404: "Não encontrado",
  409: "Conflito",
  500: "Erro interno do servidor",
};

export class ProtecError extends Error {
  /**
   * @param {string} message
   * @param {{status?: number, url?: string, body?: any, cause?: unknown}} [details]
   */
  constructor(message, details = {}) {
    super(message);
    this.name = "ProtecError";
    this.status = details.status ?? 0; // 0 = falha de rede ou timeout
    this.url = details.url ?? null;
    this.body = details.body ?? null;
    if (details.cause !== undefined) {
      this.cause = details.cause;
    }
  }

  get isNetworkError() {
    return this.status === 0;
  }

  get isBadRequest() {
    return this.status === 400;
  }

  get isNotFound() {
    return this.status === 404;
  }

  get isConflict() {
    return this.status === 409;
  }
}

function buildQuery(params) {
  const search = new URLSearchParams();

  if (!params) {
    return "";
  }

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

function segment(value) {
  return encodeURIComponent(String(value));
}

/**
 * @param {ClientOptions} [options]
 */
export function createProtecClient(options = {}) {
  const baseUrl = (options.baseUrl ?? DEFAULT_BASE_URL).replace(/\/+$/, "");
  const timeoutMs = options.timeoutMs ?? DEFAULT_TIMEOUT_MS;
  const fetchImpl = options.fetch ?? globalThis.fetch;

  if (typeof fetchImpl !== "function") {
    throw new Error("fetch não disponível: informe options.fetch");
  }

  async function request(method, path, { query, signal } = {}) {
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

    let response;
    try {
      response = await fetchImpl(url, {
        method,
        headers: { Accept: "application/json" },
        signal: controller.signal,
      });
    } catch (cause) {
      const timedOut = controller.signal.aborted && !(signal && signal.aborted);
      const message = timedOut
        ? "Tempo esgotado ao chamar " + url
        : "Falha de rede ao chamar " + url;
      throw new ProtecError(message, { url, cause });
    } finally {
      clearTimeout(timer);
    }

    const text = await response.text();
    let body = null;
    if (text !== "") {
      try {
        body = JSON.parse(text);
      } catch (e) {
        body = text;
      }
    }

    if (!response.ok) {
      const message = STATUS_MESSAGES[response.status] ?? "Erro HTTP " + response.status;
      throw new ProtecError(message, { status: response.status, url, body });
    }

    return body;
  }

  // ── Máquinas ──────────────────────────────────────────────────────────────

  const maquinas = {
    /** @param {{signal?: AbortSignal}} [opts] @returns {Promise<Maquina[]>} */
    listAll(opts) {
      return request("GET", "/maquinas", opts);
    },

    /** @param {string} id @returns {Promise<Maquina>} */
    getById(id, opts) {
      return request("GET", "/maquinas/" + segment(id), opts);
    },

    /** @param {string} nome @returns {Promise<Maquina>} */
    getByNome(nome, opts) {
      return request("GET", "/maquinas/nome/" + segment(nome), opts);
    },

    /** @param {{nome: string, riscos: string}} data @returns {Promise<Maquina>} */
    create({ nome, riscos }, opts) {
      return request("POST", "/maquinas", { ...opts, query: { nome, riscos } });
    },

    /** @param {string} id @param {string} nome @returns {Promise<Maquina>} */
    updateNome(id, nome, opts) {
      return request("PATCH", "/maquinas/" + segment(id) + "/nome", { ...opts, query: { nome } });
    },

    /** @param {string} id @param {string} riscos @returns {Promise<Maquina>} */
    updateRiscos(id, riscos, opts) {
      return request("PATCH", "/maquinas/" + segment(id) + "/riscos", { ...opts, query: { riscos } });
    },

    /** @param {string} nome @param {string} newNome @returns {Promise<Maquina>} */
    updateNomeByNome(nome, newNome, opts) {
      return request("PATCH", "/maquinas/nome/" + segment(nome) + "/nome", { ...opts, query: { newNome } });
    },

    /** @param {string} nome @param {string} riscos @returns {Promise<Maquina>} */
    updateRiscosByNome(nome, riscos, opts) {
      return request("PATCH", "/maquinas/nome/" + segment(nome) + "/riscos", { ...opts, query: { riscos } });
    },

    /** @param {string} id @returns {Promise<null>} */
    delete(id, opts) {
      return request("DELETE", "/maquinas/" + segment(id), opts);
    },

    /** @param {string} nome @returns {Promise<null>} */
    deleteByNome(nome, opts) {
      return request("DELETE", "/maquinas/nome/" + segment(nome), opts);
    },
  };

  // ── Funcionários ──────────────────────────────────────────────────────────

  const funcionarios = {
    /** @returns {Promise<Funcionario[]>} */
    listAll(opts) {
      return request("GET", "/funcionarios", opts);
    },

    /** @param {string} id @returns {Promise<Funcionario>} */
    getById(id, opts) {
      return request("GET", "/funcionarios/" + segment(id), opts);
    },

    /** @param {string} nome @returns {Promise<Funcionario>} */
    getByNome(nome, opts) {
      return request("GET", "/funcionarios/nome/" + segment(nome), opts);
    },

    /** @param {{nome: string}} data @returns {Promise<Funcionario>} */
    create({ nome }, opts) {
      return request("POST", "/funcionarios", { ...opts, query: { nome } });
    },

    /** @param {string} id @param {string} nome @returns {Promise<Funcionario>} */
    updateNome(id, nome, opts) {
      return request("PATCH", "/funcionarios/" + segment(id) + "/nome", { ...opts, query: { nome } });
    },

    /** @param {string} nome @param {string} newNome @returns {Promise<Funcionario>} */
    updateNomeByNome(nome, newNome, opts) {
      return request("PATCH", "/funcionarios/nome/" + segment(nome) + "/nome", { ...opts, query: { newNome } });
    },

    /** @param {string} id @returns {Promise<null>} */
    delete(id, opts) {
      return request("DELETE", "/funcionarios/" + segment(id), opts);
    },

    /** @param {string} nome @returns {Promise<null>} */
    deleteByNome(nome, opts) {
      return request("DELETE", "/funcionarios/nome/" + segment(nome), opts);
    },
  };

  // ── Consentimentos ────────────────────────────────────────────────────────

  const consentimentos = {
    /** @returns {Promise<Consentimento[]>} */
    listAll(opts) {
      return request("GET", "/consentimentos", opts);
    },

    /** @param {number} id @returns {Promise<Consentimento>} */
    getById(id, opts) {
      return request("GET", "/consentimentos/" + segment(id), opts);
    },

    /**
     * Último aceite por funcionário OU por máquina (exatamente um dos dois).
     * Por máquina, é o último aceite de qualquer funcionário.
     * @param {{funcionarioId?: string, maquinaId?: string}} filtro
     * @returns {Promise<Consentimento>}
     */
    getUltimo({ funcionarioId, maquinaId } = {}, opts) {
      const temFuncionario = funcionarioId !== undefined && funcionarioId !== null;
      const temMaquina = maquinaId !== undefined && maquinaId !== null;

      if (temFuncionario === temMaquina) {
        throw new TypeError("getUltimo: informe funcionarioId OU maquinaId (apenas um)");
      }

      return request("GET", "/consentimentos/ultimo", {
        ...opts,
        query: { funcionarioId, maquinaId },
      });
    },

    /** @param {string} funcionarioId @returns {Promise<Consentimento>} */
    getUltimoByFuncionario(funcionarioId, opts) {
      return consentimentos.getUltimo({ funcionarioId }, opts);
    },

    /** @param {string} maquinaId @returns {Promise<Consentimento>} */
    getUltimoByMaquina(maquinaId, opts) {
      return consentimentos.getUltimo({ maquinaId }, opts);
    },

    /**
     * Histórico do funcionário, mais recente primeiro. page começa em 0 (size máx. 100 no servidor).
     * @param {string} funcionarioId
     * @param {{page?: number, size?: number, signal?: AbortSignal}} [params]
     * @returns {Promise<Consentimento[]>}
     */
    listByFuncionario(funcionarioId, params = {}) {
      const { page, size, signal } = params;
      return request("GET", "/consentimentos/funcionario/" + segment(funcionarioId), {
        signal,
        query: { page, size },
      });
    },

    /**
     * Histórico da máquina, mais recente primeiro. page começa em 0 (size máx. 100 no servidor).
     * @param {string} maquinaId
     * @param {{page?: number, size?: number, signal?: AbortSignal}} [params]
     * @returns {Promise<Consentimento[]>}
     */
    listByMaquina(maquinaId, params = {}) {
      const { page, size, signal } = params;
      return request("GET", "/consentimentos/maquina/" + segment(maquinaId), {
        signal,
        query: { page, size },
      });
    },

    /**
     * Registra o aceite do termo. oculos/bota = CA de cada EPI (até 20 caracteres, opcionais).
     * @param {{funcionarioId: string, maquinaId: string, oculos?: string, bota?: string}} data
     * @returns {Promise<Consentimento>}
     */
    create({ funcionarioId, maquinaId, oculos, bota }, opts) {
      return request("POST", "/consentimentos", {
        ...opts,
        query: { funcionarioId, maquinaId, oculos, bota },
      });
    },
  };

  // ── Health ────────────────────────────────────────────────────────────────

  const health = {
    /** @returns {Promise<Health>} */
    get(opts) {
      return request("GET", "/health", opts);
    },
  };

  return { maquinas, funcionarios, consentimentos, health };
}

export default createProtecClient;