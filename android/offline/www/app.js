/* =========================================================
   Quotation Generator - offline edition
   Everything runs on the phone. Data stays in localStorage.
   ========================================================= */

/* ---------- Layouts (mirrors LayoutType enum) ---------- */
var LAYOUTS = [
  { name: "CLASSIC",    header: "banner",    table: "bordered", accent: "#2c3e50",  dark: false },
  { name: "MODERN",     header: "underline", table: "striped",  accent: "#2563eb",  dark: false },
  { name: "COMPACT",    header: "minimal",   table: "bordered", accent: "#475569",  dark: false },
  { name: "MINIMAL",    header: "minimal",   table: "lines",    accent: "#111827",  dark: false },
  { name: "ELEGANT",    header: "underline", table: "lines",    accent: "#14532d",  dark: false },
  { name: "BOLD",       header: "banner",    table: "bordered", accent: "#dc2626",  dark: false },
  { name: "CORPORATE",  header: "boxed",     table: "bordered", accent: "#1e3a8a",  dark: false },
  { name: "CREATIVE",   header: "split",     table: "striped",  accent: "#c026d3",  dark: false },
  { name: "MONO",       header: "minimal",   table: "minimal",  accent: "#374151",  dark: false },
  { name: "LUXURY",     header: "boxed",     table: "minimal",  accent: "#b45309",  dark: false },
  { name: "TECH",       header: "split",     table: "striped",  accent: "#0891b2",  dark: false },
  { name: "WARM",       header: "underline", table: "striped",  accent: "#ea580c",  dark: false },
  { name: "SLATE",      header: "boxed",     table: "bordered", accent: "#334155",  dark: false },
  { name: "PAPER",      header: "minimal",   table: "lines",    accent: "#78350f",  dark: false },
  { name: "OCEAN",      header: "banner",    table: "striped",  accent: "#0369a1",  dark: false },
  { name: "FOREST",     header: "underline", table: "lines",    accent: "#166534",  dark: false },
  { name: "MIDNIGHT",   header: "banner",    table: "minimal",  accent: "#1d4ed8",  dark: true },
  { name: "ROYAL",      header: "boxed",     table: "bordered", accent: "#6d28d9",  dark: false }
];

/* ---------- Storage ---------- */
var KS = { quotes: "qg.quotes", customers: "qg.customers", products: "qg.products", profile: "qg.profile" };

function load(key, fallback) {
  try { var v = JSON.parse(localStorage.getItem(key)); return v == null ? fallback : v; }
  catch (e) { return fallback; }
}
function save(key, val) { localStorage.setItem(key, JSON.stringify(val)); }

function getQuotes()  { return load(KS.quotes, []); }
function getCusts()   { return load(KS.customers, []); }
function getProds()   { return load(KS.products, []); }
function getProfile() { return load(KS.profile, { companyName: "Your Company Name", address: "", phone: "", email: "", logo: "" }); }

function putQuotes(q) { save(KS.quotes, q); }
function putCusts(c)  { save(KS.customers, c); }
function putProds(p)  { save(KS.products, p); }

/* ---------- Money helpers (3 decimals, HALF_UP) ---------- */
function money(n) {
  n = Number(n) || 0;
  var r = Math.round((n + Number.EPSILON) * 1000) / 1000;
  return r.toFixed(3);
}
function fmt(n) { return money(n); }

/* ---------- Quotation numbering (RYL-YYYY-NNN) ---------- */
function nextNumber() {
  var year = new Date().getFullYear();
  var quotes = getQuotes();
  var max = 0;
  var re = new RegExp("RYL-" + year + "-(\\d{3})");
  quotes.forEach(function (q) {
    var m = String(q.number || "").match(re);
    if (m) { var s = parseInt(m[1], 10); if (s > max) max = s; }
  });
  return "RYL-" + year + "-" + String(max + 1).padStart(3, "0");
}

/* ---------- Calculations ---------- */
function lineCalc(item) {
  var qty = Number(item.quantity) || 0;
  var price = Number(item.unitPrice) || 0;
  var disc = Number(item.discount) || 0;
  var gross = qty * price;
  var net = gross - disc;
  if (net < 0) net = 0;
  var vatPct = Number(item.vatPercent) || 0;
  var tax = net * vatPct / 100;
  item.lineNet = money(net);
  item.taxAmount = money(tax);
  item.lineTotal = money(net + tax);
  return item;
}

function recalc() {
  var rows = document.querySelectorAll("#itemsBody tr");
  var subtotal = 0, vat = 0;
  rows.forEach(function (tr) {
    var idx = tr.getAttribute("data-idx");
    var item = {
      quantity: el("i_qty" + idx).value,
      unitPrice: el("i_price" + idx).value,
      discount: el("i_disc" + idx).value || "0",
      vatPercent: el("i_vat" + idx).value || "0"
    };
    lineCalc(item);
    el("i_net" + idx).value = item.lineNet;
    el("i_tax" + idx).value = item.taxAmount;
    el("i_tot" + idx).value = item.lineTotal;
    subtotal += parseFloat(item.lineNet);
    vat += parseFloat(item.taxAmount);
  });
  var apply = el("f_applyVat").checked;
  var showVat = apply ? vat : 0;
  el("sumSubtotal").textContent = money(subtotal);
  el("sumVat").textContent = money(showVat);
  el("sumTotal").textContent = money(subtotal + showVat);
}

/* ---------- DOM helpers ---------- */
function doPrint() {
  if (window.AndroidPrint) { window.AndroidPrint.print(); } else { window.print(); }
}
function el(id) { return document.getElementById(id); }
function esc(s) { return String(s == null ? "" : s).replace(/[&<>"']/g, function (c) {
  return { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c];
}); }
function toast(msg) {
  var t = el("toast");
  t.textContent = msg; t.classList.add("show");
  clearTimeout(t._tm); t._tm = setTimeout(function () { t.classList.remove("show"); }, 2200);
}
function today() {
  var d = new Date(); var m = String(d.getMonth() + 1).padStart(2, "0"); var dd = String(d.getDate()).padStart(2, "0");
  return d.getFullYear() + "-" + m + "-" + dd;
}

/* ---------- Navigation ---------- */
function showPage(name) {
  document.querySelectorAll(".page").forEach(function (p) { p.classList.remove("active"); });
  document.querySelectorAll(".tab").forEach(function (t) { t.classList.remove("active"); });
  el("page-" + name).classList.add("active");
  var tab = document.querySelector('.tab[data-page="' + name + '"]');
  if (tab) tab.classList.add("active");
  if (name === "quotations") renderList();
  if (name === "customers") renderCustomers();
  if (name === "products") renderProducts();
  if (name === "company") fillCompany();
}

/* =========================================================
   QUOTATIONS
   ========================================================= */
function renderList() {
  var q = getQuotes();
  var term = (el("qSearch").value || "").toLowerCase();
  var box = el("quotesList");
  if (q.length === 0) {
    box.innerHTML = '<div class="empty"><div class="big">📄</div>No quotations yet.<br><button class="btn btn-primary" style="margin-top:.8rem" onclick="openForm()">+ Create your first quotation</button></div>';
    return;
  }
  q.sort(function (a, b) { return (b.id || 0) - (a.id || 0); });
  var html = "";
  q.forEach(function (qu, i) {
    if (term && !(String(qu.number).toLowerCase().indexOf(term) >= 0 ||
        (qu.customer && String(qu.customer.name).toLowerCase().indexOf(term) >= 0))) return;
    var cust = qu.customer ? qu.customer.name : "—";
    html += '<div class="q-item">' +
      '<div style="flex:1;cursor:pointer" onclick="viewQuotation(' + i + ')">' +
      '<div class="num">' + esc(qu.number) + '</div>' +
      '<div class="meta">' + esc(cust) + ' · ' + esc(qu.date) + ' · Total ' + fmt(qu.total) + ' OMR</div>' +
      '</div>' +
      '<span class="badge badge-' + String(qu.status || "DRAFT").toLowerCase() + '">' + esc(qu.status) + '</span>' +
      '<button class="btn btn-sm btn-outline" onclick="openForm(' + i + ')">✏️</button>' +
      '<button class="btn btn-sm btn-danger" onclick="deleteQuotation(' + i + ')">🗑</button>' +
      '</div>';
  });
  box.innerHTML = html || '<div class="empty">No matches for "' + esc(term) + '"</div>';
}

function fillLayouts(sel) {
  var html = "";
  LAYOUTS.forEach(function (l) { html += '<option value="' + l.name + '">' + l.name + '</option>'; });
  sel.innerHTML = html;
}

function openForm(idx) {
  fillLayouts(el("f_layout"));
  el("f_id").value = idx == null ? "" : idx;
  el("formTitle").textContent = idx == null ? "New Quotation" : "Edit Quotation";
  if (idx == null) {
    el("f_number").value = nextNumber();
    el("f_date").value = today();
    el("f_validUntil").value = "";
    el("f_status").value = "DRAFT";
    el("f_layout").value = "CLASSIC";
    el("f_custName").value = ""; el("f_custCompany").value = ""; el("f_custPhone").value = ""; el("f_custEmail").value = ""; el("f_custAddress").value = "";
    el("f_notes").value = "";
    el("f_applyVat").checked = false;
    el("itemsBody").innerHTML = "";
    addItem();
  } else {
    var q = getQuotes()[idx];
    el("f_number").value = q.number;
    el("f_date").value = q.date;
    el("f_validUntil").value = q.validUntil || "";
    el("f_status").value = q.status || "DRAFT";
    el("f_layout").value = q.layoutType || "CLASSIC";
    el("f_custName").value = (q.customer && q.customer.name) || "";
    el("f_custCompany").value = (q.customer && q.customer.companyName) || "";
    el("f_custPhone").value = (q.customer && q.customer.phone) || "";
    el("f_custEmail").value = (q.customer && q.customer.email) || "";
    el("f_custAddress").value = (q.customer && q.customer.address) || "";
    el("f_notes").value = q.notes || "";
    el("f_applyVat").checked = !!q.applyVat;
    el("itemsBody").innerHTML = "";
    (q.items || []).forEach(function (it) { addItem(it); });
  }
  el("vatRateLabel").textContent = fmt((getProfile().vatRate || 0) * 100);
  showPage("form");
  recalc();
}

var itemCounter = 0;
function addItem(it) {
  it = it || { description: "", quantity: 1, unitPrice: 0, discount: 0, vatPercent: 0 };
  var idx = itemCounter++;
  var tr = document.createElement("tr");
  tr.setAttribute("data-idx", idx);
  tr.innerHTML =
    '<td class="item-desc"><input id="i_desc' + idx + '" list="productList" placeholder="Item" value="' + esc(it.description) + '" oninput="descChanged(' + idx + ')"></td>' +
    '<td><input id="i_qty' + idx + '" type="number" min="1" step="1" value="' + esc(it.quantity) + '" oninput="recalc()"></td>' +
    '<td><input id="i_price' + idx + '" type="number" min="0" step="0.001" value="' + esc(it.unitPrice) + '" oninput="recalc()"></td>' +
    '<td><input id="i_disc' + idx + '" type="number" min="0" step="0.001" value="' + esc(it.discount || 0) + '" oninput="recalc()"></td>' +
    '<td><input id="i_vat' + idx + '" type="number" min="0" max="100" step="0.01" value="' + esc(it.vatPercent || 0) + '" oninput="recalc()"></td>' +
    '<td><input id="i_net' + idx + '" readonly value="0.000" style="background:#f8fafc"></td>' +
    '<td style="display:none"><input id="i_tax' + idx + '" readonly></td>' +
    '<td style="display:none"><input id="i_tot' + idx + '" readonly></td>' +
    '<td style="width:26px"><button class="btn btn-sm btn-danger" style="padding:.2rem .45rem" onclick="removeItem(this)">✕</button></td>';
  el("itemsBody").appendChild(tr);
  recalc();
}
function removeItem(btn) { btn.closest("tr").remove(); recalc(); }
function descChanged(idx) {
  var val = el("i_desc" + idx).value;
  getProds().forEach(function (p) {
    if (p.name === val) {
      el("i_price" + idx).value = p.price;
      el("i_vat" + idx).value = p.vatPercent || 0;
      recalc();
    }
  });
}

function collectItems() {
  var items = [];
  document.querySelectorAll("#itemsBody tr").forEach(function (tr) {
    var idx = tr.getAttribute("data-idx");
    var desc = el("i_desc" + idx).value.trim();
    if (!desc) return;
    var it = {
      description: desc,
      quantity: Number(el("i_qty" + idx).value) || 0,
      unitPrice: Number(el("i_price" + idx).value) || 0,
      discount: Number(el("i_disc" + idx).value) || 0,
      vatPercent: Number(el("i_vat" + idx).value) || 0
    };
    lineCalc(it);
    items.push(it);
  });
  return items;
}

function saveQuotation() {
  var items = collectItems();
  if (items.length === 0) { toast("⚠️ Add at least one item"); return; }
  if (!el("f_custName").value.trim()) { toast("⚠️ Customer name is required"); return; }
  var idx = el("f_id").value;
  var q = idx === "" ? {} : getQuotes()[parseInt(idx, 10)];
  q.number = el("f_number").value;
  q.date = el("f_date").value || today();
  q.validUntil = el("f_validUntil").value || "";
  q.status = el("f_status").value;
  q.layoutType = el("f_layout").value;
  q.customer = {
    name: el("f_custName").value.trim(),
    companyName: el("f_custCompany").value.trim(),
    phone: el("f_custPhone").value.trim(),
    email: el("f_custEmail").value.trim(),
    address: el("f_custAddress").value.trim()
  };
  q.notes = el("f_notes").value.trim();
  q.applyVat = el("f_applyVat").checked;
  q.vatRate = getProfile().vatRate || 0.05;
  q.items = items;
  var subtotal = 0, vat = 0;
  items.forEach(function (it) { subtotal += parseFloat(it.lineNet); vat += parseFloat(it.taxAmount); });
  q.subtotal = fmt(subtotal);
  q.taxAmount = fmt(q.applyVat ? vat : 0);
  q.total = fmt(parseFloat(q.subtotal) + parseFloat(q.taxAmount));
  q.updatedAt = new Date().toISOString();
  if (idx === "") {
    q.id = Date.now();
    q.createdAt = new Date().toISOString();
    var all = getQuotes(); all.push(q); putQuotes(all);
    toast("Quotation " + q.number + " created ✔");
  } else {
    var all2 = getQuotes(); all2[parseInt(idx, 10)] = q; putQuotes(all2);
    toast("Quotation " + q.number + " saved ✔");
  }
  renderList();
  showPage("quotations");
}

function deleteQuotation(idx) {
  var q = getQuotes()[idx];
  if (!confirm("Delete quotation " + q.number + "?")) return;
  var all = getQuotes(); all.splice(idx, 1); putQuotes(all);
  toast("Deleted"); renderList();
}

function findLayout(name) {
  for (var i = 0; i < LAYOUTS.length; i++) if (LAYOUTS[i].name === name) return LAYOUTS[i];
  return LAYOUTS[0];
}

function viewQuotation(idx) {
  var q = getQuotes()[idx];
  if (!q) return;
  var l = findLayout(q.layoutType);
  var prof = getProfile();
  var cls = "header-" + l.header + " table-" + l.table + (l.dark ? " is-dark" : "");
  var itemsHtml = "";
  (q.items || []).forEach(function (it, n) {
    itemsHtml += "<tr>" +
      "<td>" + (n + 1) + "</td>" +
      "<td><strong>" + esc(it.description) + "</strong></td>" +
      '<td class="text-center">' + it.quantity + "</td>" +
      '<td class="text-end">' + fmt(it.unitPrice) + "</td>" +
      '<td class="text-end">' + (Number(it.discount) ? fmt(it.discount) : "-") + "</td>" +
      '<td class="text-center">' + (Number(it.vatPercent) ? it.vatPercent + "%" : "-") + "</td>" +
      '<td class="text-end">' + fmt(it.taxAmount) + "</td>" +
      '<td class="text-end">' + fmt(it.lineTotal) + "</td></tr>";
  });
  if (!itemsHtml) itemsHtml = '<tr><td colspan="8" class="q-muted">No items</td></tr>';
  var cust = q.customer || {};
  var notes = q.notes ? '<div class="q-label">Notes</div><div class="q-note-box">' + esc(q.notes) + "</div>" : "";
  var vatRow = (q.applyVat && Number(q.taxAmount) > 0)
    ? '<div class="q-tot-row"><span>VAT</span><span>' + fmt(q.taxAmount) + " OMR</span></div>" : "";
  var logo = prof.logo ? '<img class="q-logo" src="' + prof.logo + '" alt="logo">' : "";

  el("quoteDoc").innerHTML =
    '<div class="q-btnbar no-print">' +
      '<button class="btn btn-outline" onclick="showPage(\'quotations\')">← Back</button>' +
      '<button class="btn btn-outline" onclick="openForm(' + idx + ')">Edit</button>' +
      '<select class="btn" style="border:1.5px solid #1d4ed8;color:#1d4ed8" onchange="changeLayout(' + idx + ', this.value)">' + layoutOptions(q.layoutType) + '</select>' +
      '<button class="btn btn-success" onclick="doPrint()">🖨 Print</button>' +
    "</div>" +
    '<div class="q-doc ' + cls + '" style="--accent:' + l.accent + '">' +
    '<div class="q-card">' +
      '<div class="q-head"><div class="q-head-main">' +
        '<div class="q-title">Quotation</div>' +
        '<div class="q-number">No: ' + esc(q.number) + "</div>" +
      '</div><div class="q-head-meta">' +
        "<div><strong>Date:</strong> " + esc(q.date) + "</div>" +
        (q.validUntil ? "<div><strong>Valid Until:</strong> " + esc(q.validUntil) + "</div>" : "") +
      "</div></div>" +
      '<div class="q-parties">' +
        '<div class="q-party"><div class="q-label">From</div>' + logo +
          '<div class="q-company">' + esc(prof.companyName) + "</div>" +
          (prof.address ? '<div class="q-detail">' + esc(prof.address) + "</div>" : "") +
          (prof.phone ? '<div class="q-detail">Phone: ' + esc(prof.phone) + "</div>" : "") +
          (prof.email ? '<div class="q-detail">Email: ' + esc(prof.email) + "</div>" : "") +
        "</div>" +
        '<div class="q-party"><div class="q-label">Bill To</div>' +
          '<div class="q-company">' + esc(cust.name || "") + "</div>" +
          (cust.companyName ? '<div class="q-detail">' + esc(cust.companyName) + "</div>" : "") +
          (cust.address ? '<div class="q-detail">' + esc(cust.address) + "</div>" : "") +
          (cust.phone ? '<div class="q-detail">Phone: ' + esc(cust.phone) + "</div>" : "") +
          (cust.email ? '<div class="q-detail">' + esc(cust.email) + "</div>" : "") +
        "</div>" +
      "</div>" +
      '<div class="q-table-wrap"><table class="q-table"><thead><tr>' +
        '<th style="width:5%">#</th><th>Description</th><th style="width:8%">Qty</th>' +
        '<th style="width:13%">Unit Price</th><th style="width:12%">Discount</th>' +
        '<th style="width:8%">VAT</th><th style="width:11%">Tax (OMR)</th><th style="width:13%">Total (OMR)</th>' +
      "</tr></thead><tbody>" + itemsHtml + "</tbody></table></div>" +
      '<div class="q-bottom"><div class="q-notes">' + notes + "</div>" +
        '<div class="q-totals">' +
          '<div class="q-tot-row"><span>Subtotal</span><span>' + fmt(q.subtotal) + " OMR</span></div>" +
          vatRow +
          '<div class="q-tot-row q-tot-grand"><span>Total</span><span>' + fmt(q.total) + " OMR</span></div>" +
        "</div></div>" +
      '<div class="q-foot"><span>Layout: <strong>' + esc(q.layoutType) + "</strong></span>" +
        "<span>Status: <strong>" + esc(q.status) + "</strong></span></div>" +
    "</div></div>";
  showPage("view");
}

function layoutOptions(selected) {
  var html = "";
  LAYOUTS.forEach(function (l) {
    html += '<option value="' + l.name + '"' + (l.name === selected ? " selected" : "") + ">" + l.name + "</option>";
  });
  return html;
}

function changeLayout(idx, layout) {
  var all = getQuotes();
  all[idx].layoutType = layout;
  putQuotes(all);
  viewQuotation(idx);
}

/* =========================================================
   CUSTOMERS
   ========================================================= */
function renderCustomers() {
  var list = getCusts();
  var box = el("customersList");
  if (list.length === 0) { box.innerHTML = '<div class="empty">No customers yet</div>'; return; }
  var html = "";
  list.forEach(function (c, i) {
    html += '<div class="q-item"><div style="flex:1"><div class="num">' + esc(c.name) + "</div>" +
      '<div class="meta">' + esc(c.companyName || c.phone || c.email || "") + "</div></div>" +
      '<button class="btn btn-sm btn-outline" onclick="editCustomer(' + i + ')">✏️</button>' +
      '<button class="btn btn-sm btn-danger" onclick="deleteCustomer(' + i + ')">🗑</button></div>';
  });
  box.innerHTML = html;
  fillCustomerDatalist();
}
function fillCustomerDatalist() {
  var html = "";
  getCusts().forEach(function (c) { html += '<option value="' + esc(c.name) + '">'; });
  el("customerList").innerHTML = html;
}
function addCustomerForm() { editCustomer(-1); }
function editCustomer(idx) {
  var box = el("customerFormBox");
  var c = idx >= 0 ? getCusts()[idx] : {};
  box.innerHTML =
    '<div class="card"><h2 class="page-title">' + (idx >= 0 ? "Edit" : "New") + " Customer</h2>" +
    '<input type="hidden" id="c_idx" value="' + idx + '">' +
    '<div class="field"><label>Name *</label><input id="c_name" value="' + esc(c.name) + '"></div>' +
    '<div class="field"><label>Company</label><input id="c_company" value="' + esc(c.companyName) + '"></div>' +
    '<div class="row2"><div class="field"><label>Phone</label><input id="c_phone" value="' + esc(c.phone) + '"></div>' +
    '<div class="field"><label>Email</label><input id="c_email" value="' + esc(c.email) + '"></div></div>' +
    '<div class="field"><label>Address</label><input id="c_address" value="' + esc(c.address) + '"></div>' +
    '<div class="btn-row"><button class="btn btn-primary" onclick="saveCustomer()">Save</button>' +
    '<button class="btn btn-outline" onclick="renderCustomers();el(\'customerFormBox\').innerHTML=\'\'">Cancel</button></div></div>';
  box.scrollIntoView({ behavior: "smooth", block: "start" });
}
function saveCustomer() {
  var idx = parseInt(el("c_idx").value, 10);
  var c = { name: el("c_name").value.trim(), companyName: el("c_company").value.trim(), phone: el("c_phone").value.trim(), email: el("c_email").value.trim(), address: el("c_address").value.trim() };
  if (!c.name) { toast("Name required"); return; }
  var all = getCusts();
  if (idx >= 0) all[idx] = c; else all.push(c);
  putCusts(all);
  el("customerFormBox").innerHTML = "";
  toast("Customer saved"); renderCustomers();
}
function deleteCustomer(idx) {
  if (!confirm("Delete customer?")) return;
  var all = getCusts(); all.splice(idx, 1); putCusts(all);
  renderCustomers();
}

/* =========================================================
   PRODUCTS
   ========================================================= */
function renderProducts() {
  var list = getProds();
  var box = el("productsList");
  if (list.length === 0) { box.innerHTML = '<div class="empty">No products yet</div>'; return; }
  var html = "";
  list.forEach(function (p, i) {
    html += '<div class="q-item"><div style="flex:1"><div class="num">' + esc(p.name) + "</div>" +
      '<div class="meta">' + fmt(p.price) + " OMR" + (p.vatPercent ? " · VAT " + p.vatPercent + "%" : "") + "</div></div>" +
      '<button class="btn btn-sm btn-outline" onclick="editProduct(' + i + ')">✏️</button>' +
      '<button class="btn btn-sm btn-danger" onclick="deleteProduct(' + i + ')">🗑</button></div>';
  });
  box.innerHTML = html;
  fillProductDatalist();
}
function fillProductDatalist() {
  var html = "";
  getProds().forEach(function (p) { html += '<option value="' + esc(p.name) + '">'; });
  el("productList").innerHTML = html;
}
function addProductForm() { editProduct(-1); }
function editProduct(idx) {
  var box = el("productFormBox");
  var p = idx >= 0 ? getProds()[idx] : {};
  box.innerHTML =
    '<div class="card"><h2 class="page-title">' + (idx >= 0 ? "Edit" : "New") + " Product</h2>" +
    '<input type="hidden" id="p_idx" value="' + idx + '">' +
    '<div class="field"><label>Name *</label><input id="pr_name" value="' + esc(p.name) + '"></div>' +
    '<div class="row2"><div class="field"><label>Price (OMR)</label><input id="pr_price" type="number" min="0" step="0.001" value="' + esc(p.price) + '"></div>' +
    '<div class="field"><label>VAT %</label><input id="pr_vat" type="number" min="0" max="100" step="0.01" value="' + esc(p.vatPercent) + '"></div></div>' +
    '<div class="btn-row"><button class="btn btn-primary" onclick="saveProduct()">Save</button>' +
    '<button class="btn btn-outline" onclick="renderProducts();el(\'productFormBox\').innerHTML=\'\'">Cancel</button></div></div>';
  box.scrollIntoView({ behavior: "smooth", block: "start" });
}
function saveProduct() {
  var idx = parseInt(el("p_idx").value, 10);
  var p = { name: el("pr_name").value.trim(), price: Number(el("pr_price").value) || 0, vatPercent: Number(el("pr_vat").value) || 0 };
  if (!p.name) { toast("Name required"); return; }
  var all = getProds();
  if (idx >= 0) all[idx] = p; else all.push(p);
  putProds(all);
  el("productFormBox").innerHTML = "";
  toast("Product saved"); renderProducts();
}
function deleteProduct(idx) {
  if (!confirm("Delete product?")) return;
  var all = getProds(); all.splice(idx, 1); putProds(all);
  renderProducts();
}

/* =========================================================
   COMPANY PROFILE
   ========================================================= */
function fillCompany() {
  var p = getProfile();
  el("p_name").value = p.companyName || "";
  el("p_address").value = p.address || "";
  el("p_phone").value = p.phone || "";
  el("p_email").value = p.email || "";
  el("p_vat").value = (p.vatRate || 0.05) * 100;
  if (p.logo) { el("p_logoPreview").src = p.logo; el("p_logoPreview").style.display = "block"; }
  else { el("p_logoPreview").style.display = "none"; }
}
function logoPicked(input) {
  var f = input.files && input.files[0];
  if (!f) return;
  if (f.size > 1024 * 1024) { toast("Logo too large (max 1 MB)"); input.value = ""; return; }
  var reader = new FileReader();
  reader.onload = function (e) {
    el("p_logoPreview").src = e.target.result;
    el("p_logoPreview").style.display = "block";
  };
  reader.readAsDataURL(f);
}
function removeLogo() {
  el("p_logoPreview").style.display = "none";
  el("p_logo").value = "";
  el("p_logoPreview").removeAttribute("src");
}
function saveCompany() {
  var p = getProfile();
  p.companyName = el("p_name").value.trim() || "Your Company Name";
  p.address = el("p_address").value.trim();
  p.phone = el("p_phone").value.trim();
  p.email = el("p_email").value.trim();
  p.vatRate = (Number(el("p_vat").value) || 5) / 100;
  if (el("p_logoPreview").src && el("p_logoPreview").style.display !== "none") p.logo = el("p_logoPreview").src;
  else if (!el("p_logo").files.length && !el("p_logoPreview").getAttribute("src")) p.logo = "";
  save(KS.profile, p);
  toast("Profile saved ✔");
}

/* ---------- init ---------- */
(function init() {
  if (!localStorage.getItem(KS.profile)) save(KS.profile, { companyName: "Your Company Name", address: "", phone: "", email: "", logo: "", vatRate: 0.05 });
  renderList();
})();