# Demo de la API - Operacion Alquimia (Windows PowerShell)
# 1) En otra terminal: .\mvnw.cmd spring-boot:run
# 2) Aqui:            powershell -ExecutionPolicy Bypass -File .\demo.ps1
# El laboratorio de ejemplo esta en (100, 100) y el mensaje es "este es un mensaje secreto".

$base = "http://localhost:8080/api/v1"
$ErrorActionPreference = "Stop"

function Call($method, $path, $body = $null) {
    $p = @{ Method = $method; Uri = "$base$path"; ContentType = "application/json" }
    if ($body -ne $null) { $p["Body"] = ConvertTo-Json -InputObject $body -Depth 6 }
    try {
        $r = Invoke-RestMethod @p
        if ($r -ne $null) { ConvertTo-Json -InputObject $r -Depth 6 | Write-Host }
        else { Write-Host "(sin contenido)" }
    } catch {
        $code = [int]$_.Exception.Response.StatusCode
        Write-Host "HTTP $code -> $($_.ErrorDetails.Message)" -ForegroundColor Red
    }
}

function Step($title) { Write-Host ""; Write-Host "=== $title ===" -ForegroundColor Cyan; Read-Host "Enter para ejecutar" | Out-Null }

$labs = @{ distance = 670.8203932499369; message = @("este", "", "", "mensaje", "") }
$ccpd = @{ distance = 200; message = @("", "es", "un", "", "") }
$sat  = @{ distance = 400; message = @("este", "", "", "", "secreto") }

# Limpia lo que haya quedado de una ejecucion anterior
foreach ($d in "STAR_LABS", "CCPD", "STAR_SATELLITE") { try { Invoke-RestMethod -Method Delete -Uri "$base/devices/$d" | Out-Null } catch {} }

Step "F1 - Procesar la informacion completa de los tres dispositivos (no guarda nada)"
Call "POST" "/analysis" @{ devices = @(
    @{ deviceId = "STAR_LABS";      distance = $labs.distance; message = $labs.message },
    @{ deviceId = "CCPD";           distance = $ccpd.distance; message = $ccpd.message },
    @{ deviceId = "STAR_SATELLITE"; distance = $sat.distance;  message = $sat.message } ) }

Step "F2 - Registrar dispositivos de forma independiente y en cualquier orden"
Call "POST" "/devices" @{ deviceId = "STAR_SATELLITE"; distance = $sat.distance; message = $sat.message }
Call "POST" "/devices" @{ deviceId = "STAR_LABS"; distance = $labs.distance; message = $labs.message }

Step "F3 - Consultar lo acumulado (falta CCPD) e intentar procesar: no hay datos suficientes"
Call "GET" "/devices"
Call "GET" "/analysis"

Step "F2 - Registrar el que faltaba y volver a procesar lo acumulado"
Call "POST" "/devices" @{ deviceId = "CCPD"; distance = $ccpd.distance; message = $ccpd.message }
Call "GET" "/analysis"

Step "F2 - Registrar un dispositivo repetido: conflicto (para eso existen PUT y PATCH)"
Call "POST" "/devices" @{ deviceId = "CCPD"; distance = 1; message = @("x") }

Step "F4 - Reemplazo completo de CCPD (los otros dispositivos no cambian)"
Call "PUT" "/devices/CCPD" @{ distance = 200; message = @("", "fue", "un", "", "") }
Call "GET" "/analysis"

Step "F5 - Actualizacion parcial: solo la distancia (el fragmento se conserva)"
Call "PATCH" "/devices/CCPD" @{ distance = 201.5 }
Call "GET" "/devices/CCPD"

Step "F5 - Actualizacion parcial: solo el fragmento (la distancia se conserva)"
Call "PATCH" "/devices/CCPD" @{ message = @("", "es", "un", "", "") }
Call "GET" "/devices/CCPD"

Step "F6 - Eliminar CCPD (los otros siguen) y comprobar que ya no hay datos suficientes"
Call "DELETE" "/devices/CCPD"
Call "GET" "/devices"
Call "GET" "/analysis"

Step "Validaciones: dispositivo desconocido, distancia negativa y actualizacion vacia"
Call "GET" "/devices/BATCAVE"
Call "POST" "/devices" @{ deviceId = "CCPD"; distance = -5; message = @("x") }
Call "PATCH" "/devices/STAR_LABS" @{}

Write-Host ""; Write-Host "Demo terminada." -ForegroundColor Cyan
