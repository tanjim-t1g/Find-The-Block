// Copy code button helper
function copyText(text) {
  navigator.clipboard.writeText(text).then(() => {
    const btns = document.querySelectorAll('.copy-btn');
    btns.forEach(btn => {
      if (btn.innerText === 'Copy') {
        btn.innerText = 'Copied';
        setTimeout(() => { btn.innerText = 'Copy'; }, 1800);
      }
    });
  });
}

// Search Modal Functionality
(function() {
  const rootPath = document.body.getAttribute('data-root') || '';

  const searchIndex = [
    { title: 'Overview', href: rootPath + 'index.html', snippet: 'Scavenger-rush minigame plugin for Paper 1.21+.' },
    { title: 'Installation & Quick Start', href: rootPath + 'getting-started.html', snippet: 'Deploying the jar file and initial configuration.' },
    { title: 'World Factory & Chunk Pacing', href: rootPath + 'architecture/world-factory.html', snippet: 'Zero-lag asynchronous world creation and chunk queues.' },
    { title: 'Match Lifecycle & State Machine', href: rootPath + 'architecture/lifecycle.html', snippet: 'Finite state machine: LOBBY, COUNTDOWN, ACTIVE, PODIUM, RESETTING.' },
    { title: 'Reconnect & Mid-Game Flow', href: rootPath + 'architecture/reconnect-engine.html', snippet: '60-second crash grace window and late-joining racers.' },
    { title: 'Survival Milestone Buffs', href: rootPath + 'architecture/survival-buffs.html', snippet: 'Speed II and Regeneration I buffs replacing Elytras at 20 minutes.' },
    { title: 'Void Lobby Generation', href: rootPath + 'configuration/void-lobby.html', snippet: 'Built-in 100x100 bedrock platform and void boundary at Y=90.' },
    { title: 'Custom Lobby Setup', href: rootPath + 'configuration/custom-lobby.html', snippet: 'Configuring pre-built hubs and multi-world managers.' },
    { title: 'Difficulty Tiers', href: rootPath + 'configuration/difficulty-tiers.html', snippet: 'EASY, NORMAL, and HARD item pools and recipe guide.' },
    { title: 'Configuration Reference', href: rootPath + 'configuration/config-reference.html', snippet: 'Complete annotated config.yml reference.' },
    { title: 'Commands & Permissions', href: rootPath + 'reference/commands.html', snippet: 'Command table, aliases, and permission nodes.' },
    { title: 'PlaceholderAPI & Leaderboards', href: rootPath + 'reference/placeholderapi.html', snippet: 'Registered placeholders and persistent leaderboard tracking.' },
    { title: 'Troubleshooting & FAQ', href: rootPath + 'reference/troubleshooting.html', snippet: 'Resolving common operational and configuration questions.' }
  ];

  const searchModal = document.getElementById('searchModal');
  const openSearchBtn = document.getElementById('openSearch');
  const searchInput = document.getElementById('searchInput');
  const searchResults = document.getElementById('searchResults');

  if (!searchModal || !openSearchBtn) return;

  function renderResults(query) {
    searchResults.innerHTML = '';
    const q = query.toLowerCase().trim();
    const filtered = q ? searchIndex.filter(item => 
      item.title.toLowerCase().includes(q) || item.snippet.toLowerCase().includes(q)
    ) : searchIndex;

    if (filtered.length === 0) {
      searchResults.innerHTML = '<div style="padding: 16px; color: #666; font-size: 0.85rem; text-align: center;">No matching documentation found.</div>';
      return;
    }

    filtered.forEach(item => {
      const el = document.createElement('a');
      el.className = 'search-result-item';
      el.href = item.href;
      el.innerHTML = `<div class="search-result-title">${item.title}</div><div class="search-result-snippet">${item.snippet}</div>`;
      el.onclick = () => { closeSearch(); };
      searchResults.appendChild(el);
    });
  }

  function openSearch() {
    searchModal.classList.add('open');
    searchInput.value = '';
    renderResults('');
    searchInput.focus();
  }

  function closeSearch() {
    searchModal.classList.remove('open');
  }

  openSearchBtn.onclick = openSearch;

  window.addEventListener('keydown', (e) => {
    if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
      e.preventDefault();
      if (searchModal.classList.contains('open')) closeSearch();
      else openSearch();
    }
    if (e.key === 'Escape' && searchModal.classList.contains('open')) {
      closeSearch();
    }
  });

  searchModal.addEventListener('click', (e) => {
    if (e.target === searchModal) closeSearch();
  });

  searchInput.addEventListener('input', (e) => {
    renderResults(e.target.value);
  });

  // TOC scroll spy
  const headings = document.querySelectorAll('main.content h2, main.content h3');
  const tocLinks = document.querySelectorAll('.toc-item a');

  if (headings.length > 0 && tocLinks.length > 0) {
    window.addEventListener('scroll', () => {
      let current = '';
      const scrollPos = window.scrollY + 100;

      headings.forEach(heading => {
        const top = heading.offsetTop;
        if (scrollPos >= top - 20) {
          current = heading.getAttribute('id');
        }
      });

      if (current) {
        tocLinks.forEach(link => {
          link.parentElement.classList.toggle('active', link.getAttribute('href') === '#' + current);
        });
      }
    });
  }
})();
