package com.ws.bitesmart.controller.payment;

import com.ws.bitesmart.service.payment.AlipayPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/payment/alipay")
@RequiredArgsConstructor
public class AlipayPaymentController {
    private final AlipayPaymentService alipayPaymentService;

    @PostMapping(value = "/notify", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String notify(@RequestParam Map<String, String> params) {
        alipayPaymentService.handleNotify(params);
        return "success";
    }
}
