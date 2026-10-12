// SwipeHire client script. Vanilla JS, kept under 100 lines on purpose: everything also works
// without it (every action is a normal form POST). It only adds 1) live WebSocket alerts and
// chat, 2) the drag-to-swipe gesture on cards.

// ---------- 1. WebSocket: live notifications + chat ----------
(function () {
  if (!document.body.dataset.ws) return;                       // only for logged-in pages
  var proto = location.protocol === 'https:' ? 'wss://' : 'ws://';
  var ws = new WebSocket(proto + location.host + document.body.dataset.ctx + '/ws/notify');
  ws.onmessage = function (event) {
    var m = JSON.parse(event.data);
    if (m.type === 'chat') {
      var box = document.getElementById('chat-' + m.matchId);
      if (box) {                                               // conversation is open: append bubble
        var div = document.createElement('div');
        div.className = 'msg';
        div.innerHTML = '<div class="meta"></div><div class="body"></div>';
        div.querySelector('.meta').textContent = m.sender + ' · ' + m.sentAt;
        div.querySelector('.body').textContent = m.body;      // textContent = no HTML injection
        box.appendChild(div);
        box.scrollTop = box.scrollHeight;
        return;
      }
      toast('New message from ' + m.sender, null);            // chat elsewhere: toast only
      return;
    }
    var count = document.getElementById('notif-count');
    if (count) { count.textContent = (parseInt(count.textContent || '0', 10) + 1); count.removeAttribute('data-zero'); }
    if (m.kind !== 'MESSAGE') toast(m.title, m.link);          // the chat push above already toasted
  };

  function toast(text, link) {
    var t = document.createElement('div');
    t.className = 'toast';
    t.textContent = text + ' ';
    if (link) {
      var a = document.createElement('a');
      a.href = document.body.dataset.ctx + link;
      a.textContent = 'Open';
      t.appendChild(a);
    }
    document.body.appendChild(t);
    setTimeout(function () { t.remove(); }, 7000);
  }

  var chat = document.querySelector('.chat');
  if (chat) chat.scrollTop = chat.scrollHeight;
})();

// ---------- 2. Drag-to-swipe on the top card ----------
(function () {
  var card = document.getElementById('swipe-card');
  if (!card) return;
  var like = card.querySelector('.stamp.like'), nope = card.querySelector('.stamp.nope');
  var startX = null, dx = 0, THRESHOLD = 120;

  card.addEventListener('pointerdown', function (e) {
    if (e.target.closest('button, input, a, textarea, select')) return;  // let buttons work normally
    startX = e.clientX;
    card.classList.add('dragging');
    card.setPointerCapture(e.pointerId);
  });

  card.addEventListener('pointermove', function (e) {
    if (startX === null) return;
    dx = e.clientX - startX;
    card.style.transform = 'translateX(' + dx + 'px) rotate(' + dx / 20 + 'deg)';
    if (like) like.style.opacity = Math.max(0, Math.min(1, dx / THRESHOLD));
    if (nope) nope.style.opacity = Math.max(0, Math.min(1, -dx / THRESHOLD));
  });

  function release() {
    if (startX === null) return;
    card.classList.remove('dragging');
    startX = null;
    var formId = dx > THRESHOLD ? card.dataset.right : dx < -THRESHOLD ? card.dataset.left : null;
    if (formId) {                                              // fly off, then submit the same form a button would
      card.style.transform = 'translateX(' + (dx > 0 ? 1000 : -1000) + 'px) rotate(' + dx / 10 + 'deg)';
      document.getElementById(formId).submit();
    } else {
      card.style.transform = '';
      if (like) like.style.opacity = 0;
      if (nope) nope.style.opacity = 0;
    }
    dx = 0;
  }
  card.addEventListener('pointerup', release);
  card.addEventListener('pointercancel', release);
})();
