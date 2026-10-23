import createProtecClient from "./client.js";

const api = createProtecClient({ baseUrl: "http://localhost:8081", timeoutMs: 9000 });
const $ = id => document.getElementById(id);
const state = { employees: [], machines: [], consents: [], online: false, toastTimer: null, unlocked: false };
const ADMIN_PIN = "2468"; // Troque o PIN antes de colocar em uso.
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
function fillEmployeeSuggestions() {
  $("employeeOptions").innerHTML=state.employees.map(e=>`<option value="${esc(e.nome)}"></option>`).join("");
}
function selectedEmployee() {
  const name=$("employeeInput").value.trim().toLocaleLowerCase("pt-BR");
  return state.employees.find(e=>e.nome.trim().toLocaleLowerCase("pt-BR")===name);
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
$("employeeInput").addEventListener("input",()=>{ $("employeeInput").setCustomValidity(""); });
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
$("clearForm").addEventListener("click",()=>{ $("consentForm").reset(); $("formError").hidden=true; $("employeeInput").setCustomValidity(""); renderRisk(); });
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
    fillEmployeeSuggestions(); fillMachineSelect(); renderRisk(); renderLists(); renderHistory();
    setStatus(true,"API conectada");
  } catch(err) {
    setStatus(false,"API indisponível");
    $("employeeOptions").innerHTML="";
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
  if(!employee){$("employeeInput").setCustomValidity("Selecione um nome existente na lista de funcionários cadastrados.");$("employeeInput").reportValidity();error.textContent="Funcionário não encontrado. Escolha um nome cadastrado ou solicite o cadastro à administração.";error.hidden=false;return;}
  if(!machine){error.textContent="Selecione uma máquina.";error.hidden=false;return;}
  if(!machine.riscos?.trim()){error.textContent="Esta máquina não tem riscos descritos. Peça à Segurança do Trabalho para completar o cadastro antes de continuar.";error.hidden=false;return;}
  if(!$("readRisks").checked||!$("usePPE").checked||!$("truthful").checked){error.textContent="Marque todas as declarações após ler as informações.";error.hidden=false;return;}
  const btn=$("submitConsent");btn.disabled=true;btn.textContent="Registrando…";
  try {
    await api.consentimentos.create({funcionarioId:employee.id,maquinaId:machine.id,oculos:$("glassesCA").value.trim()||undefined,bota:$("bootsCA").value.trim()||undefined});
    await loadData(); $("consentForm").reset();renderRisk();toast("Consentimento registrado com sucesso.");
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
