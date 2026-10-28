# TODO fMysteriousBoxes

## Migração para Paper 1.21 (24/10/2026)
- `config.yml` das caixas no servidor: a chave `data` dos prémios deixou de existir e os materiais do 1.8 que já não existem dão aviso; atualizar à mão.
- `Prize.data` (sempre 0) e o construtor deprecated `ItemStack(mat, int, short)` em `Box.java` por limpar.

- Reload de config exige `/freload plugin` (o `BoxRegistry` é lido uma vez e partilhado com listener e comando).
