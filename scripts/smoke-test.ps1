$ErrorActionPreference = 'Continue'
$base = 'http://localhost:8080/api'
$user = '13800000001'
$merchant = '13800000002'
$headers = @{ Authorization = $user; 'Content-Type' = 'application/json' }
$results = @()

function Invoke-Api {
    param(
        [string]$Method = 'GET',
        [string]$Path,
        [hashtable]$Hdr = $headers,
        [object]$Body = $null
    )
    $uri = "$base$Path"
    try {
        if ($Body -ne $null) {
            $json = ($Body | ConvertTo-Json -Depth 6 -Compress)
            $resp = Invoke-WebRequest -Uri $uri -Method $Method -Headers $Hdr -Body $json -UseBasicParsing -TimeoutSec 15
        } else {
            $resp = Invoke-WebRequest -Uri $uri -Method $Method -Headers $Hdr -UseBasicParsing -TimeoutSec 15
        }
        $data = $resp.Content | ConvertFrom-Json
        return @{ ok = $true; code = $data.code; message = $data.message; data = $data.data; raw = $data }
    } catch {
        $msg = $_.Exception.Message
        if ($_.Exception.Response) {
            try {
                $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
                $msg = $reader.ReadToEnd()
            } catch {}
        }
        return @{ ok = $false; error = $msg }
    }
}

function Msg($r) {
    if ($r.message) { return $r.message }
    if ($r.error) { return $r.error }
    return ''
}

function Record([string]$name, [bool]$pass, [string]$detail) {
    $script:results += [pscustomobject]@{ Test = $name; Pass = $pass; Detail = $detail }
    $flag = if ($pass) { 'PASS' } else { 'FAIL' }
    Write-Host "[$flag] $name :: $detail"
}

Write-Host '=== CLAS Smoke Test ==='

$login = Invoke-Api -Method POST -Path '/user/login' -Hdr @{} -Body @{ phone = $user; password = 'Abc123!' }
Record '用户登录' ($login.ok -and $login.code -eq 200) (Msg $login)

$profile = Invoke-Api -Path '/user/profile'
Record '获取个人资料' ($profile.ok -and $profile.code -eq 200) "nickname=$($profile.data.nickname)"
$origNick = $profile.data.nickname
$testNick = "测试昵称$(Get-Date -Format 'HHmmss')"
$upd = Invoke-Api -Method PUT -Path '/user/profile' -Body @{ nickname = $testNick }
Record '更新昵称' ($upd.ok -and $upd.code -eq 200 -and $upd.data.nickname -eq $testNick) (Msg $upd)
if ($upd.ok) {
    Invoke-Api -Method PUT -Path '/user/profile' -Body @{ nickname = $origNick } | Out-Null
}

$pen = Invoke-Api -Path '/user/penalties/mine'
Record '处罚记录列表' ($pen.ok -and $pen.code -eq 200) "count=$($pen.data.Count)"
$appeals = Invoke-Api -Path '/user/appeals/mine'
Record '申诉记录列表' ($appeals.ok -and $appeals.code -eq 200) "count=$($appeals.data.Count)"
$appeal = Invoke-Api -Method POST -Path '/user/appeals' -Body @{ content = 'smoke test appeal' }
Record '提交申诉' ($appeal.ok -and $appeal.code -eq 200) (Msg $appeal)

$notifs = Invoke-Api -Path '/notifications/mine'
Record '通知列表' ($notifs.ok -and $notifs.code -eq 200) "count=$($notifs.data.Count)"
$readAll = Invoke-Api -Method POST -Path '/notifications/read-all'
Record '全部已读' ($readAll.ok -and $readAll.code -eq 200) (Msg $readAll)
if ($notifs.data -and $notifs.data.Count -gt 0) {
    $nid = $notifs.data[0].id
    $delOne = Invoke-Api -Method DELETE -Path "/notifications/$nid"
    Record '删除单条通知' ($delOne.ok -and $delOne.code -eq 200) (Msg $delOne)
} else {
    Record '删除单条通知' $true '无通知可删，跳过'
}
$delAll = Invoke-Api -Method DELETE -Path '/notifications/all'
Record '清空通知' ($delAll.ok -and $delAll.code -eq 200) (Msg $delAll)

$reviews = Invoke-Api -Path '/review/merchant/1'
Record '商家评价列表' ($reviews.ok -and $reviews.code -eq 200) "count=$($reviews.data.Count)"
if ($reviews.data -and $reviews.data.Count -gt 0) {
    $rid = $reviews.data[0].id
    $vote = Invoke-Api -Method POST -Path "/review/REVIEW/$rid/vote" -Body @{ voteType = 'LIKE' }
    Record '评价点赞' ($vote.ok -and $vote.code -eq 200) (Msg $vote)
    $vote2 = Invoke-Api -Method POST -Path "/review/REVIEW/$rid/vote" -Body @{ voteType = 'DISLIKE' }
    Record '评价点踩切换' ($vote2.ok -and $vote2.code -eq 200) (Msg $vote2)
} else {
    Record '评价点赞/点踩' $false '商家1无评价数据，无法测试投票'
}

$preview = Invoke-Api -Path ("/order/preview?merchantId=1" + "&addressId=1")
Record '订单结算预览' ($preview.ok -and $preview.code -eq 200) "deliveryFee=$($preview.data.deliveryFee) minOrder=$($preview.data.minOrderAmount)"

$coupons = Invoke-Api -Path '/coupon/claimable'
if ($coupons.ok -and $coupons.code -eq 200) {
    Record '可领优惠券' $true "count=$($coupons.data.Count)"
    if ($coupons.data.Count -gt 0) {
        $cid = $coupons.data[0].id
        $claim = Invoke-Api -Method POST -Path "/coupon/claim/$cid"
        Record '领取优惠券' ($claim.ok -and $claim.code -eq 200) (Msg $claim)
    } else {
        Record '领取优惠券' $true '无可领券，跳过'
    }
} else {
    Record '可领优惠券' $false (Msg $coupons)
}

Invoke-Api -Method POST -Path '/cart/add' -Body @{ userId = $user; productId = 2; quantity = 1 } | Out-Null
$create = Invoke-Api -Method POST -Path '/order/create' -Body @{ userId = $user; merchantId = 1; addressId = 1; remark = 'smoke test remark' }
Record '创建订单(含备注)' ($create.ok -and $create.code -eq 200) "status=$($create.data.order.status) remark=$($create.data.order.remark)"
if ($create.ok -and $create.code -eq 200) {
    $oid = $create.data.order.id
    $pay = Invoke-Api -Method POST -Path '/payment/mock' -Body @{ orderId = $oid; userId = $user; payMethod = 'MOCK' }
    Record '模拟支付' ($pay.ok -and $pay.code -eq 200) "payment=$($pay.data.paymentStatus)"
}

$mHeaders = @{ Authorization = $merchant; 'Content-Type' = 'application/json' }
Invoke-Api -Method POST -Path '/cart/add' -Body @{ userId = $user; productId = 2; quantity = 1 } | Out-Null
$create2 = Invoke-Api -Method POST -Path '/order/create' -Body @{ userId = $user; merchantId = 1; addressId = 1 }
if ($create2.ok -and $create2.code -eq 200) {
    $oid2 = $create2.data.order.id
    $pay2 = Invoke-Api -Method POST -Path '/payment/mock' -Body @{ orderId = $oid2; userId = $user; payMethod = 'MOCK' }
    if ($pay2.ok -and $pay2.code -eq 200) {
        $reject = Invoke-Api -Method POST -Path "/order/reject/$oid2" -Hdr $mHeaders -Body @{ reason = '库存不足测试拒单' }
        Record '商家拒单(含理由)' ($reject.ok -and $reject.code -eq 200) "status=$($reject.data.status) reason=$($reject.data.rejectReason)"
    } else {
        Record '商家拒单(含理由)' $false '支付失败'
    }
} else {
    Record '商家拒单(含理由)' $false (Msg $create2)
}

$bundle = @(
    (Invoke-Api -Path '/address/list'),
    (Invoke-Api -Path '/deal-order/mine'),
    (Invoke-Api -Path '/favorite/list'),
    (Invoke-Api -Path '/notifications/mine'),
    (Invoke-Api -Path '/user/penalties/mine'),
    (Invoke-Api -Path '/user/appeals/mine')
)
$bundleOk = ($bundle | Where-Object { $_.ok -and $_.code -eq 200 }).Count
Record '个人中心并行加载' ($bundleOk -ge 4) "$bundleOk/6 接口成功"

Write-Host ''
Write-Host '=== Summary ==='
$passed = ($results | Where-Object Pass).Count
$failed = ($results | Where-Object { -not $Pass })
Write-Host "通过: $passed / $($results.Count)"
if ($failed.Count -gt 0) {
    Write-Host '失败项:'
    $failed | ForEach-Object { Write-Host "  - $($_.Test): $($_.Detail)" }
}
