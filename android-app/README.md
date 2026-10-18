# Dojo LB Graduação - Android App

Aplicativo nativo Android para avaliação técnica de alunos durante exames de graduação de Jiu-Jitsu.

## Tecnologias

- **Kotlin** + **Jetpack Compose** (Material3)
- **Room Database** (SQLite) para armazenamento local offline
- **iText7** para geração de PDF
- **Kotlinx Serialization** para backup/import JSON
- **Android Sharesheet** para compartilhamento de arquivos
- **Arquitetura MVVM** com Repository pattern

## Funcionalidades

### Cadastro de Alunos
- Nome completo
- Faixa atual e faixa desejada (Branca → Preta)
- Data do exame
- Quantidade de posições configurável (1-50)

### Avaliação Técnica
- Lista numerada de posições com nomes editáveis
- 5 botões de nota por posição: 0%, 25%, 50%, 75%, 100%
- Seleção única por posição com destaque visual
- Salvamento automático
- Contador de posições avaliadas/pendentes
- Média provisória em tempo real
- Adicionar/remover posições dinamicamente

### Cálculo e Regras
- Média aritmética simples de todas as posições
- **Aprovação: média > 75% (estritamente maior)**
- **Reprovação: média ≤ 75%**
- Resultado só calculado quando todas as posições avaliadas
- Média provisória considera apenas posições avaliadas

### Finalização
- Resumo visual completo antes de confirmar
- Impede finalização com posições pendentes
- Permite reabrir exame finalizado (com recálculo)

### Exportação
- **PDF** profissional com layout do Dojo LB
- **CSV** UTF-8 com BOM, separador `;` (compatível Excel BR)
- Exportação individual e geral (todos os alunos)
- Compartilhamento via Android Sharesheet

### Backup/Import
- Backup completo em ZIP (JSON)
- Importação com validação de duplicatas
- Opção de sobrescrever ou pular existentes

### Offline-First
- 100% funcional sem internet
- Dados persistem localmente (Room/SQLite)
- Android Auto Backup configurado

## Requisitos

- Android 7.0 (API 24) ou superior
- Java 17 / Kotlin 1.9+
- Gradle 8.5+

## Build

```bash
# Navegar para o diretório do projeto Android
cd android-app

# Dar permissão de execução (Linux/Mac)
chmod +x gradlew

# Build debug APK
./gradlew assembleDebug

# Build release APK (requer keystore configurado)
./gradlew assembleRelease

# Executar testes
./gradlew test
```

## Estrutura do Projeto

```
android-app/
├── app/
│   ├── src/main/
│   │   ├── java/br/com/dojolb/graduacao/
│   │   │   ├── data/
│   │   │   │   ├── entity/       # Student, Position
│   │   │   │   ├── dao/          # StudentDao, PositionDao
│   │   │   │   ├── database/     # AppDatabase
│   │   │   │   ├── repository/   # GraduacaoRepository
│   │   │   │   └── backup/       # BackupManager
│   │   │   ├── ui/
│   │   │   │   ├── theme/        # Color, Type, Theme
│   │   │   │   ├── components/   # CommonComponents
│   │   │   │   ├── viewmodel/    # StudentViewModel, EvaluationViewModel, ReportViewModel
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── student/StudentDetailActivity.kt
│   │   │   │   ├── evaluation/EvaluationActivity.kt
│   │   │   │   └── report/ReportActivity.kt
│   │   │   ├── DojoApplication.kt
│   │   │   └── BackupAgent.kt
│   │   ├── res/
│   │   │   ├── values/           # strings, colors, themes
│   │   │   ├── drawable/         # Icons, splash
│   │   │   ├── xml/              # file_paths, backup_rules
│   │   │   └── mipmap-*/         # App icons
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew / gradlew.bat
└── gradle/wrapper/
```

## Testes

Os testes cobrem os 12 cenários obrigatórios:

1. ✅ Média 100% → Aprovado
2. ✅ Média 80% → Aprovado
3. ✅ Média 75% → Reprovado (regra estrita > 75)
4. ✅ Média 50% → Reprovado
5. ✅ Avaliação incompleta → Em andamento
6. ✅ Alterar nota atualiza média
7. ✅ Dados persistem após fechar/reabrir
8. ✅ Exportação contém dados corretos
9. ✅ PDF apresenta resultado correto
10. ✅ Exame com 1 posição funciona
11. ✅ Alunos independentes
12. ✅ Notas alteradas após conclusão atualizam resultado

Executar: `./gradlew test`

## Identidade Visual

- **Preto** (#000000) - Cor predominante
- **Vermelho** (#C62828) - Destaque principal
- **Branco** (#FFFFFF) - Textos e contraste
- **Cinza-escuro** (#1E1E1E) - Cards, campos, divisórias

## Permissões

- `INTERNET` - Apenas para potencial uso futuro (não usada atualmente)
- `READ/WRITE_EXTERNAL_STORAGE` - Para exportação de arquivos (API ≤ 28)
- `READ_MEDIA_*` - Para acesso a arquivos exportados (API 29+)

## Assinatura Release

Configure `signingConfigs` no `app/build.gradle.kts` com seu keystore:

```kotlin
signingConfigs {
    create("release") {
        storeFile = file("keystore.jks")
        storePassword = "store_password"
        keyAlias = "key_alias"
        keyPassword = "key_password"
    }
}
```

## Instalação no Dispositivo

1. Conecte o celular via USB com depuração USB ativada
2. Execute: `./gradlew installDebug`
3. Ou transfira o APK gerado em `app/build/outputs/apk/debug/app-debug.apk`
4. Instale manualmente no Android (permita instalação de fontes desconhecidas)

## Limitações Conhecidas

- Ícones de launcher são placeholders vetoriais simples
- PDF generation requer iText7 (licença AGPL - verifique conformidade para uso comercial)
- Não há sincronização em nuvem (por design - offline-first)
- Testes de UI (androidTest) requerem device/emulator conectado

## Licença

Projeto interno Dojo LB - Uso restrito à organização.