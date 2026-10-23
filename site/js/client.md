# Cliente JS da API PROTEC

Cliente REST para a API do PROTEC (Spring Boot). Arquivo único, sem dependências, usa `fetch` (navegador ou Node 18+) e é um ES Module com tipos em JSDoc.

- **Arquivo:** `protec-client.js`
- **Cobertura:** todos os endpoints REST da API (máquinas, funcionários, consentimentos e health)
- **Fora do escopo:** WebSocket e vídeo (ainda não existem no backend)

## Sumário

1. [Instalação e uso](#instalação-e-uso)
2. [Configuração](#configuração)
3. [Tipos de dados](#tipos-de-dados)
4. [Referência](#referência)
5. [Erros](#erros)
6. [Timeout e cancelamento](#timeout-e-cancelamento)
7. [Consentimentos: regras importantes](#consentimentos-regras-importantes)
8. [Exemplos](#exemplos)
9. [Observações](#observações)

---

## Instalação e uso

Copie o `protec-client.js` para o projeto e importe:

```js
import { createProtecClient } from "./protec-client.js";

const api = createProtecClient({ baseUrl: "http://localhost:8081" });

const maquinas = await api.maquinas.listAll();
```

Para o Node tratar o arquivo como módulo, use `"type": "module"` no `package.json` (ou a extensão `.mjs`). Em bundlers (Next.js, Vite) funciona sem ajustes.

Há também um export `default`, equivalente ao nomeado:

```js
import createProtecClient from "./protec-client.js";
```

---

## Configuração

```js
createProtecClient({ baseUrl, timeoutMs, fetch })
```

| Opção | Tipo | Padrão | Descrição |
|---|---|---|---|
| `baseUrl` | `string` | `http://localhost:8081` | URL base da API. Barras no final são removidas. |
| `timeoutMs` | `number` | `10000` | Tempo máximo de cada requisição, em milissegundos. |
| `fetch` | `function` | `globalThis.fetch` | Implementação de `fetch` (testes ou Node antigo). |

Se não houver `fetch` disponível e nenhuma implementação for passada, `createProtecClient` lança um `Error`.

O cliente retornado tem quatro grupos: `api.maquinas`, `api.funcionarios`, `api.consentimentos` e `api.health`.

---

## Tipos de dados

Definidos como `@typedef` no arquivo, para autocomplete no editor.

### `Maquina`

| Campo | Tipo |
|---|---|
| `id` | `string` (UUID) |
| `nome` | `string` |
| `riscos` | `string` |

### `Funcionario`

| Campo | Tipo |
|---|---|
| `id` | `string` (UUID) |
| `nome` | `string` |

### `Consentimento`

| Campo | Tipo | Descrição |
|---|---|---|
| `id` | `number` | Identificador do aceite (inteiro). |
| `funcionarioId` | `string` | UUID do funcionário. |
| `maquinaId` | `string` | UUID da máquina. |
| `oculos` | `string \| null` | CA do óculos. |
| `bota` | `string \| null` | CA da bota. |
| `data` | `string` | Momento do aceite (ISO 8601). |
| `expiraEm` | `string` | Fim da validade do aceite (ISO 8601). |
| `valido` | `boolean` | `true` enquanto o aceite estiver dentro da validade. |
| `segundosRestantes` | `number` | Tempo restante de validade, `0` se vencido. |

### `Health`

| Campo | Tipo |
|---|---|
| `status`, `service`, `version`, `environment` | `string` |
| `timestamp` | `string` (ISO 8601) |
| `javaVersion`, `springBootVersion` | `string` |
| `dependencies` | `{ name: string, status: string, latency: number }[]` |

---

## Referência

Todos os métodos retornam uma `Promise`. O último argumento de cada método pode ser `{ signal }` (um `AbortSignal`) para cancelar a chamada. Os métodos de listagem paginada recebem `signal` dentro do mesmo objeto de parâmetros.

### `api.maquinas`

| Método | Retorno | Requisição HTTP |
|---|---|---|
| `listAll()` | `Maquina[]` | `GET /maquinas` |
| `getById(id)` | `Maquina` | `GET /maquinas/{id}` |
| `getByNome(nome)` | `Maquina` | `GET /maquinas/nome/{nome}` |
| `create({ nome, riscos })` | `Maquina` | `POST /maquinas?nome=&riscos=` |
| `updateNome(id, nome)` | `Maquina` | `PATCH /maquinas/{id}/nome?nome=` |
| `updateRiscos(id, riscos)` | `Maquina` | `PATCH /maquinas/{id}/riscos?riscos=` |
| `updateNomeByNome(nome, newNome)` | `Maquina` | `PATCH /maquinas/nome/{nome}/nome?newNome=` |
| `updateRiscosByNome(nome, riscos)` | `Maquina` | `PATCH /maquinas/nome/{nome}/riscos?riscos=` |
| `delete(id)` | `null` | `DELETE /maquinas/{id}` |
| `deleteByNome(nome)` | `null` | `DELETE /maquinas/nome/{nome}` |

### `api.funcionarios`

| Método | Retorno | Requisição HTTP |
|---|---|---|
| `listAll()` | `Funcionario[]` | `GET /funcionarios` |
| `getById(id)` | `Funcionario` | `GET /funcionarios/{id}` |
| `getByNome(nome)` | `Funcionario` | `GET /funcionarios/nome/{nome}` |
| `create({ nome })` | `Funcionario` | `POST /funcionarios?nome=` |
| `updateNome(id, nome)` | `Funcionario` | `PATCH /funcionarios/{id}/nome?nome=` |
| `updateNomeByNome(nome, newNome)` | `Funcionario` | `PATCH /funcionarios/nome/{nome}/nome?newNome=` |
| `delete(id)` | `null` | `DELETE /funcionarios/{id}` |
| `deleteByNome(nome)` | `null` | `DELETE /funcionarios/nome/{nome}` |

### `api.consentimentos`

| Método | Retorno | Requisição HTTP |
|---|---|---|
| `listAll()` | `Consentimento[]` | `GET /consentimentos` |
| `getById(id)` | `Consentimento` | `GET /consentimentos/{id}` |
| `getUltimo({ funcionarioId })` ou `getUltimo({ maquinaId })` | `Consentimento` | `GET /consentimentos/ultimo?funcionarioId=` ou `?maquinaId=` |
| `getUltimoByFuncionario(funcionarioId)` | `Consentimento` | atalho para `getUltimo({ funcionarioId })` |
| `getUltimoByMaquina(maquinaId)` | `Consentimento` | atalho para `getUltimo({ maquinaId })` |
| `listByFuncionario(funcionarioId, { page, size, signal })` | `Consentimento[]` | `GET /consentimentos/funcionario/{funcionarioId}?page=&size=` |
| `listByMaquina(maquinaId, { page, size, signal })` | `Consentimento[]` | `GET /consentimentos/maquina/{maquinaId}?page=&size=` |
| `create({ funcionarioId, maquinaId, oculos, bota })` | `Consentimento` | `POST /consentimentos?funcionarioId=&maquinaId=&oculos=&bota=` |

### `api.health`

| Método | Retorno | Requisição HTTP |
|---|---|---|
| `get()` | `Health` | `GET /health` |

### Observações sobre os parâmetros

- Valores `undefined` ou `null` não são enviados na query string.
- Valores em segmentos de URL (nomes, ids) são codificados automaticamente: `"Torno CNC"` vira `Torno%20CNC`.
- Os `POST` criam registros e a API responde `201`. O cliente aceita qualquer status `2xx`.
- Respostas sem corpo (como os `DELETE`) retornam `null`.

---

## Erros

Toda resposta fora de `2xx` lança um `ProtecError`:

```js
import { ProtecError } from "./protec-client.js";

try {
  await api.maquinas.getByNome("Inexistente");
} catch (e) {
  if (e instanceof ProtecError && e.isNotFound) {
    // máquina não existe
  }
}
```

### Propriedades

| Propriedade | Descrição |
|---|---|
| `message` | Mensagem em português baseada no status (tabela abaixo). |
| `status` | Status HTTP. `0` indica falha de rede ou timeout. |
| `url` | URL completa chamada. |
| `body` | Corpo da resposta de erro, já convertido de JSON quando possível (`null` se vazio). |
| `cause` | Erro original, em falhas de rede ou timeout. |
| `isBadRequest` | `status === 400` |
| `isNotFound` | `status === 404` |
| `isConflict` | `status === 409` |
| `isNetworkError` | `status === 0` |

### Mensagens por status

| Status | Mensagem |
|---|---|
| `400` | Requisição inválida |
| `404` | Não encontrado |
| `409` | Conflito |
| `500` | Erro interno do servidor |
| outros | `Erro HTTP {status}` |
| `0` (timeout) | `Tempo esgotado ao chamar {url}` |
| `0` (rede) | `Falha de rede ao chamar {url}` |

Hoje a API devolve os erros **sem corpo** (só o status), então a mensagem de `ProtecError` vem do status, e não de um texto do servidor.

### Erro de uso

`getUltimo` lança um `TypeError` (antes de qualquer chamada de rede) se você informar os dois filtros ou nenhum.

---

## Timeout e cancelamento

Cada requisição tem timeout (`timeoutMs`, padrão 10 s). Ao estourar, lança `ProtecError` com `status === 0` e mensagem de tempo esgotado.

Para cancelar manualmente, passe um `AbortSignal`:

```js
const controller = new AbortController();

const promessa = api.maquinas.listAll({ signal: controller.signal });
controller.abort(); // cancela

// em métodos paginados, o signal vai junto dos parâmetros
api.consentimentos.listByMaquina(maquinaId, { page: 0, size: 20, signal: controller.signal });
```

Um cancelamento manual também lança `ProtecError` com `status === 0`, mas com a mensagem de falha de rede (e não de timeout).

Em React, use isso no cleanup do `useEffect` para não atualizar estado de componentes desmontados.

---

## Consentimentos: regras importantes

### Validade de 1 hora

Um aceite vale por **1 hora** a partir de `data`. O servidor calcula a validade e a devolve pronta em `valido`, `expiraEm` e `segundosRestantes`, então o front não precisa calcular nada. O horário do aceite é sempre definido pelo servidor.

### `getUltimo`: um filtro por vez

Informe **`funcionarioId` ou `maquinaId`**, nunca os dois:

```js
await api.consentimentos.getUltimo({ funcionarioId });   // último aceite do funcionário
await api.consentimentos.getUltimo({ maquinaId });       // último aceite da máquina
```

Por máquina, o resultado é o último aceite de **qualquer funcionário**. Se dois funcionários aceitarem para a mesma máquina, só o mais recente aparece.

Se não existir nenhum aceite, a API responde `404` (`isNotFound`). Se existir mas estiver vencido, responde `200` com `valido: false`.

### Histórico paginado

`listByFuncionario` e `listByMaquina` devolvem do mais recente para o mais antigo.

| Parâmetro | Padrão | Observação |
|---|---|---|
| `page` | `0` | Começa em 0. Valores negativos viram 0. |
| `size` | `20` | Entre 1 e 100. Valores fora do limite são ajustados pelo servidor. |

Se o funcionário ou a máquina não existir, a API responde `404`. Se existir mas não tiver aceites, devolve lista vazia.

`listAll` (todos os consentimentos) não é paginado.

### Registrar um aceite

`oculos` e `bota` são os **CA** de cada EPI, opcionais, com até 20 caracteres. Texto em branco é salvo como vazio. Funcionário ou máquina inexistente, ou CA longo demais, resulta em `400`.

O aceite do termo **não habilita a máquina** por si só. A decisão física depende das verificações da Raspberry Pi.

---

## Exemplos

### Cadastrar e consultar

```js
const maquina = await api.maquinas.create({ nome: "Torno CNC", riscos: "Corte, projeção de cavacos" });
const funcionario = await api.funcionarios.create({ nome: "Carlos" });

const todas = await api.maquinas.listAll();
const achada = await api.maquinas.getByNome("Torno CNC");
```

### Registrar o aceite e checar a validade

```js
const aceite = await api.consentimentos.create({
  funcionarioId: funcionario.id,
  maquinaId: maquina.id,
  oculos: "12345",
  bota: "67890",
});

console.log(aceite.valido);             // true
console.log(aceite.segundosRestantes);  // ~3600
```

### Verificar se o funcionário pode usar a máquina agora

```js
async function consentimentoValido(funcionarioId) {
  try {
    const ultimo = await api.consentimentos.getUltimo({ funcionarioId });
    return ultimo.valido;
  } catch (e) {
    if (e instanceof ProtecError && e.isNotFound) {
      return false; // nunca aceitou
    }
    throw e;
  }
}
```

### Histórico paginado

```js
let page = 0;
const size = 50;

while (true) {
  const lote = await api.consentimentos.listByMaquina(maquinaId, { page, size });
  if (lote.length === 0) break;

  processar(lote);

  if (lote.length < size) break;
  page += 1;
}
```

### Hook React com cancelamento

```js
useEffect(() => {
  const controller = new AbortController();

  api.maquinas
    .listAll({ signal: controller.signal })
    .then(setMaquinas)
    .catch((e) => {
      if (!(e instanceof ProtecError) || e.status !== 0) {
        setErro(e);
      }
    });

  return () => controller.abort();
}, []);
```

### Checar se a API está no ar

```js
const saude = await api.health.get();
console.log(saude.status, saude.dependencies);
```

---

## Observações

- **CORS:** ao chamar a API do navegador (por exemplo, pelo Next.js em outra porta), o backend precisa liberar a origem do front. Hoje o projeto Spring não tem essa configuração, e o sintoma é um `ProtecError` com `status === 0`.
- **Sem autenticação:** a primeira versão do sistema não usa login, então o cliente não envia credenciais.
- **Apagar com histórico:** funcionários e máquinas que já têm consentimentos registrados não podem ser apagados (a chave estrangeira impede). Dependendo de como o backend tratar esse caso, o `DELETE` vai falhar com um erro de status `4xx` ou `500`.
- **Endpoints por nome:** os que usam `/nome/{nome}` dependem de o nome ser único. Evite-os em código novo e prefira os por `id`.