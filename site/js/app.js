import createProtecClient from "./client.js";

const api = createProtecClient({ baseUrl: "http://localhost:8081", timeoutMs: 9000 });
const $ = id => document.getElementById(id);
const state = { employees: [], machines: [], consents: [], employee: null, online: false, toastTimer: null, unlocked: false };
const ADMIN_PIN = "2468"; 
const views = { consent: "Consentimento", admin: "Cadastros", history: "Histórico" };

function esc(v="") { return String(v).replace(/[&<>"']/g, c => ({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c])); }
function dateText(v) { if(!v) return "—"; const d=new Date(v); return Number.isNaN(d.getTime())?"—":new Intl.DateTimeFormat("pt-BR",{dateStyle:"short",timeStyle:"short"}).format(d); }
function toast(msg, error=false) { const el=$("toast"); el.textContent=msg; el.classList.toggle("error",error); el.classList.add("show"); clearTimeout(state.toastTimer); state.toastTimer=setTimeout(()=>el.classList.remove("show"),4000); }
function setStatus(ok,msg) { state.online=ok; $("apiDot").classList.toggle("online",ok); $("apiStatus").lastChild.textContent=msg; }
function navigate(view) {
  document.querySelectorAll(".page").forEach(p=>p.classList.toggle("active",p.id===view+"View"));
  document.querySelectorAll(".nav-button").forEach(b=>b.classList.toggle("active",b.dataset.view===view));
  if(view==="admin") renderLists();
  if(view==="history") renderHistory();
  window.scrollTo({top:0,behavior:"smooth"});
}
document.querySelectorAll("[data-view]").forEach(b=>b.addEventListener("click",()=>{
  const view=b.dataset.view;
  if(view!=="consent"&&!state.unlocked) {
    const pin=window.prompt("Área restrita. Digite o PIN de acesso:");
    if(pin!==ADMIN_PIN) { if(pin!==null) toast("PIN incorreto. Acesso não autorizado.",true); return; }
    state.unlocked=true;
    toast("Acesso liberado nesta sessão.");
  }
  navigate(view);
}));
function fillMachineSelect() {
  const s=$("machineSelect"), prev=s.value;
  s.innerHTML='<option value="">Selecione a máquina ou equipamento…</option>'+state.machines.map(m=>`<option value="${esc(m.id)}">${esc(m.nome)}</option>`).join("");
  if(state.machines.some(m=>m.id===prev)) s.value=prev;
}
// ── Funcionário: busca com sugestões (só seleciona; nunca cadastra) ─────────
const SUGGESTION_LIMIT=8;
const HINT_DEFAULT="Digite para buscar e escolha um funcionário da lista. Não é possível cadastrar um novo aqui.";
const suggest={ items: [], active: -1 };
const canon=s=>String(s||"").trim().toLocaleLowerCase("pt-BR");
const norm=s=>String(s||"").normalize("NFD").replace(/[\u0300-\u036f]/g,"").toLocaleLowerCase("pt-BR").trim();

function setEmployeeHint(msg,invalid=false) {
  $("employeeHint").textContent=msg;
  $("employeeHint").classList.toggle("is-error",invalid);
  $("employeeInput").classList.toggle("is-invalid",invalid);
}
function closeSuggestions() {
  const list=$("employeeSuggestions"), input=$("employeeInput");
  list.hidden=true; list.innerHTML=""; suggest.items=[]; suggest.active=-1;
  input.setAttribute("aria-expanded","false");
  input.removeAttribute("aria-activedescendant");
}
function setActiveSuggestion(index) {
  const options=[...$("employeeSuggestions").querySelectorAll(".suggestion")];
  suggest.active=index;
  options.forEach((el,i)=>{ el.classList.toggle("active",i===index); el.setAttribute("aria-selected",i===index?"true":"false"); });
  if(index>=0&&options[index]) {
    options[index].scrollIntoView({block:"nearest"});
    $("employeeInput").setAttribute("aria-activedescendant",options[index].id);
  }
}
function highlight(name,q) {
  if(!q) return esc(name);
  const n=norm(name), at=n.indexOf(q);
  if(at<0||n.length!==name.length) return esc(name);
  return esc(name.slice(0,at))+"<mark>"+esc(name.slice(at,at+q.length))+"</mark>"+esc(name.slice(at+q.length));
}
function renderSuggestions() {
  const input=$("employeeInput"), list=$("employeeSuggestions");
  const q=norm(input.value);
  let matches=state.employees.filter(e=>norm(e.nome).includes(q));
  matches.sort((a,b)=>{
    const sa=norm(a.nome).startsWith(q)?0:1, sb=norm(b.nome).startsWith(q)?0:1;
    if(sa!==sb) return sa-sb;
    return a.nome.localeCompare(b.nome,"pt-BR");
  });
  const total=matches.length;
  matches=matches.slice(0,SUGGESTION_LIMIT);
  suggest.items=matches; suggest.active=-1;
  if(matches.length===0) {
    const text=state.employees.length===0?"Nenhum funcionário cadastrado ainda.":"Nenhum funcionário encontrado. Peça o cadastro à administração.";
    list.innerHTML=`<li class="suggestion-empty" role="presentation">${text}</li>`;
  } else {
    list.innerHTML=matches.map((e,i)=>`<li class="suggestion" id="employeeOption${i}" role="option" aria-selected="false" data-index="${i}">${highlight(e.nome,q)}</li>`).join("")
      +(total>matches.length?`<li class="suggestion-more" role="presentation">+${total-matches.length} resultados. Continue digitando para filtrar.</li>`:"");
  }
  list.hidden=false;
  input.setAttribute("aria-expanded","true");
}
function chooseEmployee(employee) {
  const input=$("employeeInput");
  state.employee=employee;
  input.value=employee.nome;
  input.setCustomValidity("");
  setEmployeeHint(HINT_DEFAULT);
  closeSuggestions();
}
function resetEmployee() {
  state.employee=null;
  $("employeeInput").setCustomValidity("");
  setEmployeeHint(HINT_DEFAULT);
  closeSuggestions();
}
function refreshEmployeeSuggestions() {
  if(state.employee&&!state.employees.some(e=>e.id===state.employee.id)) state.employee=null;
  if(!$("employeeSuggestions").hidden) renderSuggestions();
}
function selectedEmployee() {
  const name=canon($("employeeInput").value);
  if(name==="") return undefined;
  if(state.employee&&canon(state.employee.nome)===name&&state.employees.some(e=>e.id===state.employee.id)) return state.employee;
  return state.employees.find(e=>canon(e.nome)===name);
}
function selectedMachine() { return state.machines.find(m=>m.id===$("machineSelect").value); }
function renderRisk() {
  const m=selectedMachine();
  $("riskBox").classList.toggle("has-risk",!!m);
  $("riskMachine").textContent=m?m.nome:"Selecione uma máquina para visualizar os riscos.";
  $("riskText").textContent=m?(m.riscos?.trim()||"Este cadastro não possui riscos descritos. Não registre o consentimento; solicite a correção à Segurança do Trabalho."):"Os riscos cadastrados aparecerão aqui.";
  $("readRisks").checked=false;
}
$("machineSelect").addEventListener("change",renderRisk);
$("employeeInput").addEventListener("input",()=>{
  state.employee=null;
  $("employeeInput").setCustomValidity("");
  setEmployeeHint(HINT_DEFAULT);
  renderSuggestions();
});
$("employeeInput").addEventListener("focus",()=>{
  if(state.employee) { $("employeeInput").select(); return; }
  renderSuggestions();
});
$("employeeInput").addEventListener("blur",()=>{
  closeSuggestions();
  if($("employeeInput").value.trim()!==""&&!selectedEmployee()) setEmployeeHint("Esse funcionário não está cadastrado. Escolha um nome da lista.",true);
});
$("employeeInput").addEventListener("keydown",ev=>{
  const list=$("employeeSuggestions");
  if(ev.key==="ArrowDown"||ev.key==="ArrowUp") {
    if(list.hidden) renderSuggestions();
    if(suggest.items.length===0) return;
    ev.preventDefault();
    const last=suggest.items.length-1;
    let next=ev.key==="ArrowDown"?suggest.active+1:suggest.active-1;
    if(next>last) next=0;
    if(next<0) next=last;
    setActiveSuggestion(next);
    return;
  }
  if(ev.key==="Enter") {
    if(!list.hidden&&suggest.active>=0) { ev.preventDefault(); chooseEmployee(suggest.items[suggest.active]); }
    return;
  }
  if(ev.key==="Escape") closeSuggestions();
});
$("employeeSuggestions").addEventListener("mousedown",ev=>{
  const item=ev.target.closest(".suggestion");
  if(!item) return;
  ev.preventDefault(); // mantém o foco no campo
  chooseEmployee(suggest.items[Number(item.dataset.index)]);
});
function employeeName(id) { return state.employees.find(e=>e.id===id)?.nome||"Funcionário não encontrado"; }
function machineName(id) { return state.machines.find(m=>m.id===id)?.nome||"Máquina não encontrada"; }
function renderLists() {
  $("employeeCount").textContent=state.employees.length;
  $("machineCount").textContent=state.machines.length;
  $("employeeList").innerHTML=state.employees.length?state.employees.map(e=>`<li>${esc(e.nome)}</li>`).join(""):'<li class="muted">Nenhum funcionário cadastrado.</li>';
  $("machineList").innerHTML=state.machines.length?state.machines.map(m=>`<li><b>${esc(m.nome)}</b><small>${esc(m.riscos||"Sem riscos descritos")}</small></li>`).join(""):'<li class="muted">Nenhuma máquina cadastrada.</li>';
}
function renderHistory() {
  const q=$("historySearch").value.trim().toLocaleLowerCase("pt-BR");
  const rows=[...state.consents].sort((a,b)=>new Date(b.data)-new Date(a.data)).filter(c=>
    employeeName(c.funcionarioId).toLocaleLowerCase("pt-BR").includes(q)||machineName(c.maquinaId).toLocaleLowerCase("pt-BR").includes(q));
  $("historySummary").textContent=`${rows.length} ${rows.length===1?"registro encontrado":"registros encontrados"}`;
  $("historyBody").innerHTML=rows.length?rows.map(c=>`<tr><td><b>${esc(employeeName(c.funcionarioId))}</b></td><td>${esc(machineName(c.maquinaId))}</td><td>${esc(dateText(c.data))}</td><td>${esc(c.oculos||"—")}</td><td>${esc(c.bota||"—")}</td><td><span class="status-pill">${c.valido===false?"Expirado":"Registrado"}</span></td></tr>`).join(""):'<tr><td colspan="6" class="empty-cell">Nenhum registro encontrado.</td></tr>';
}
$("historySearch").addEventListener("input",renderHistory);
$("refreshHistory").addEventListener("click",loadData);
$("refreshData").addEventListener("click",loadData);
$("clearForm").addEventListener("click",()=>{ $("consentForm").reset(); $("formError").hidden=true; resetEmployee(); renderRisk(); });
function errorText(err) {
  if(err?.status===0||err?.isNetworkError) return "Não foi possível conectar à API. Confira se o backend está rodando em http://localhost:8081.";
  if(err?.status===409) return "Esse cadastro já existe.";
  if(err?.status===400) return "Os dados não foram aceitos. Confira os campos.";
  return err?.message||"Ocorreu um erro inesperado.";
}
async function loadData() {
  try {
    const [employees,machines,consents]=await Promise.all([api.funcionarios.listAll(),api.maquinas.listAll(),api.consentimentos.listAll()]);
    state.employees=employees||[]; state.machines=machines||[]; state.consents=consents||[];
    refreshEmployeeSuggestions(); fillMachineSelect(); renderRisk(); renderLists(); renderHistory();
    setStatus(true,"API conectada");
  } catch(err) {
    setStatus(false,"API indisponível");
    closeSuggestions();
    $("machineSelect").innerHTML='<option value="">API indisponível</option>';
    $("riskText").textContent=errorText(err);
    $("historySummary").textContent="Não foi possível carregar os registros.";
    $("historyBody").innerHTML=`<tr><td colspan="6">${esc(errorText(err))}</td></tr>`;
    toast(errorText(err),true);
  }
}
$("consentForm").addEventListener("submit",async ev=>{
  ev.preventDefault(); const error=$("formError"); error.hidden=true;
  const employee=selectedEmployee(), machine=selectedMachine();
  if(!state.online){error.textContent="A API está indisponível. O registro não foi salvo.";error.hidden=false;return;}
  if(!employee){$("employeeInput").setCustomValidity("Selecione um nome existente na lista de funcionários cadastrados.");$("employeeInput").reportValidity();setEmployeeHint("Escolha um funcionário da lista de cadastrados.",true);error.textContent="Funcionário não encontrado. Escolha um nome da lista de cadastrados; este formulário não cria novos funcionários. Solicite o cadastro à administração.";error.hidden=false;return;}
  if(!machine){error.textContent="Selecione uma máquina.";error.hidden=false;return;}
  if(!machine.riscos?.trim()){error.textContent="Esta máquina não tem riscos descritos. Peça à Segurança do Trabalho para completar o cadastro antes de continuar.";error.hidden=false;return;}
  if(!$("readRisks").checked||!$("usePPE").checked||!$("truthful").checked){error.textContent="Marque todas as declarações após ler as informações.";error.hidden=false;return;}
  const btn=$("submitConsent");btn.disabled=true;btn.textContent="Registrando…";
  try {
    await api.consentimentos.create({funcionarioId:employee.id,maquinaId:machine.id,oculos:$("glassesCA").value.trim()||undefined,bota:$("bootsCA").value.trim()||undefined});
    await loadData(); $("consentForm").reset();resetEmployee();renderRisk();toast("Consentimento registrado com sucesso.");
  } catch(err){error.textContent="Não foi possível registrar. "+errorText(err);error.hidden=false;}
  finally{btn.disabled=false;btn.innerHTML='Confirmar ciência e registrar <span>→</span>';}
});
function showFormMessage(id,msg,isError=false){const el=$(id);el.textContent=msg;el.classList.toggle("error",isError);el.hidden=false;}
$("employeeForm").addEventListener("submit",async ev=>{
  ev.preventDefault();$("employeeMessage").hidden=true;
  const nome=$("newEmployee").value.trim();
  if(!nome)return;
  try{await api.funcionarios.create({nome});$("employeeForm").reset();await loadData();showFormMessage("employeeMessage","Funcionário cadastrado com sucesso.");toast("Funcionário cadastrado.");}
  catch(err){showFormMessage("employeeMessage",errorText(err),true);}
});
$("machineForm").addEventListener("submit",async ev=>{
  ev.preventDefault();$("machineMessage").hidden=true;
  const nome=$("newMachine").value.trim(), riscos=$("newRisks").value.trim();
  if(!nome||!riscos)return;
  try{await api.maquinas.create({nome,riscos});$("machineForm").reset();await loadData();showFormMessage("machineMessage","Máquina cadastrada com sucesso.");toast("Máquina cadastrada.");}
  catch(err){showFormMessage("machineMessage",errorText(err),true);}
});
loadData();