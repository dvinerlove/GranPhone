# Instalando o vophone no celular

Guia passo a passo para deixar o celular inteiro funcionando com o vophone:
tela inicial simples, tela de ligação com botões grandes e modo quiosque
(a pessoa não consegue sair do vophone nem mexer nas configurações do Android).

Nada disso grava um sistema novo no celular: tudo pode ser desfeito.

## Do que você precisa

- O celular, com Android 6 ou mais novo (o modo quiosque fica mais completo no Android 9 ou mais novo).
- Um cabo USB e um computador.
- O `adb` no computador: baixe o "SDK Platform-Tools" do Android
  (https://developer.android.com/tools/releases/platform-tools), descompacte e abra um terminal nessa pasta.
- O APK do vophone (`app/build/outputs/apk/fDroid/debug/app-fDroid-debug.apk`, gerado com
  `./gradlew assembleFDroidDebug`).

## 1. Preparar o celular

1. Faça as configurações básicas antes de travar: Wi-Fi, volume da campainha bem alto, brilho,
   tamanho da fonte (Configurações > Tela > Tamanho da fonte, use o maior).
2. Ative as Opções do desenvolvedor: Configurações > Sobre o telefone > toque 7 vezes em
   "Número da versão" (ou "Versão MIUI" / "Número de compilação").
3. Em Configurações > Sistema > Opções do desenvolvedor, ative **Depuração USB**.
   Em celulares Xiaomi ative também "Depuração USB (configurações de segurança)".
4. Ligue o cabo e aceite a pergunta "Permitir depuração USB?" no celular.
5. No computador, confira se o celular aparece:

   ```
   adb devices
   ```

## 2. Instalar o vophone

```
adb install -r vophone-debug.apk
```

Depois, no celular:

1. Abra o vophone e aceite as permissões que ele pedir.
2. Quando perguntar qual tela inicial usar, escolha **vophone** e "Sempre".
3. No vophone, abra Configurações > **Tela de ligação com botões grandes** e aceite o vophone como
   aplicativo de telefone padrão. A partir daí as ligações recebidas e feitas aparecem com os botões
   grandes ATENDER / RECUSAR / DESLIGAR.
4. Em Configurações > Personalização > **Número de Emergência**, confira o número do botão SOS (192 por padrão).
5. Na tela SOS, cadastre os dois contatos de emergência (por exemplo, você e outro familiar).

## 3. Tornar o vophone administrador do celular (só uma vez)

O Android só deixa um app virar "dono" do aparelho quando **não há nenhuma conta** cadastrada.

1. No celular, vá em Configurações > Contas (ou "Usuários e contas") e **remova todas as contas**:
   Google, Samsung, Xiaomi, WhatsApp, etc. (os dados ficam salvos na nuvem; você adiciona de volta depois).
2. No computador:

   ```
   adb shell dpm set-device-owner com.bald.uriah.baldphone/.broadcast_receivers.BaldDeviceAdminReceiver
   ```

   Deve aparecer `Success: Device owner set to package com.bald.uriah.baldphone`.
   - Se aparecer erro falando de *accounts*: ainda tem alguma conta no celular. Remova e tente de novo.
   - Se aparecer erro falando de *users*: existe outro usuário ou "Pasta segura" / "Segundo espaço". Remova e tente de novo.
   - Se nada funcionar, o caminho garantido é restaurar o celular de fábrica, pular o login do Google
     na configuração inicial, instalar o vophone e rodar o comando.
3. Adicione as contas de volta (Configurações > Contas > Adicionar conta).

## 4. Antes de ligar o modo quiosque

Com o quiosque ligado, os pedidos de permissão de outros aplicativos podem não aparecer. Então, antes:

- Abra uma vez cada aplicativo que ele vai usar (WhatsApp, câmera, galeria...) e aceite as permissões.
- Configure o WhatsApp e confira se as notificações e o som estão funcionando.

## 5. Ligar o modo quiosque

No vophone: Configurações > **Modo quiosque (travar o celular)** > crie uma senha de pelo menos 4 números.

Com o quiosque ligado:

- O celular fica sempre no vophone. O botão Início volta para o vophone.
- A barra de notificações, a lista de apps recentes e as Configurações do Android ficam bloqueadas.
  As notificações aparecem dentro do vophone (tela de Notificações).
- Só abrem o vophone e os aplicativos que aparecem nele (menos Configurações, loja de apps e outras telas iniciais).
- Não dá para desinstalar aplicativos.
- O menu do botão de ligar continua funcionando, para reiniciar ou desligar o celular.

## Desligar o modo quiosque

- No celular: vophone > Configurações > Modo quiosque > digite a senha.
- Esqueceu a senha? Pelo computador, com o cabo:

  ```
  adb shell am broadcast -a com.bald.uriah.baldphone.action.KIOSK_OFF -n com.bald.uriah.baldphone/.broadcast_receivers.KioskAdbReceiver
  ```

## Atualizar o vophone

Gere o APK novo e rode de novo:

```
adb install -r vophone-debug.apk
```

O APK é assinado com a chave fixa `app/vophone-debug.keystore`, então qualquer build deste
repositório atualiza o vophone já instalado. Um APK assinado com outra chave não instala por cima.

## Remover o vophone de vez

1. Desligue o modo quiosque.
2. vophone > Configurações > **Remover o vophone como administrador** (ou, pelo computador:
   `adb shell am broadcast -a com.bald.uriah.baldphone.action.REMOVE_DEVICE_OWNER -n com.bald.uriah.baldphone/.broadcast_receivers.KioskAdbReceiver`).
3. Escolha outro app de telefone e outra tela inicial nas Configurações do Android e desinstale o vophone normalmente.

## Teste antes de entregar

- [ ] Ligar para o celular: aparece ATENDER / RECUSAR, inclusive com a tela bloqueada.
- [ ] Atender, testar o viva-voz, desligar.
- [ ] Ligar a partir do vophone para um contato.
- [ ] Botão SOS liga para os contatos de emergência.
- [ ] Botão de emergência: confira que abre a ligação para o número certo (cancele antes de completar).
- [ ] Alarme e lembrete de remédio tocam com a tela apagada.
- [ ] Com o quiosque ligado: arrastar a barra de cima não abre nada, o botão Início volta para o vophone.
- [ ] Reiniciar o celular: ele volta direto para o vophone travado.
