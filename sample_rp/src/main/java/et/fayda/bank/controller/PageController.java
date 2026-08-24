package et.fayda.bank.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String home() {
        return "home";
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal OidcUser user, Model model) {
        addUserAttributes(model, user);
        return "dashboard";
    }

    @GetMapping("/unverified-dashboard")
    public String unverifiedDashboard(Model model) {
        model.addAttribute("userName", "User");
        return "unverified_dashboard";
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal OidcUser user, Model model) {
        addUserAttributes(model, user);
        return "profile";
    }

    @GetMapping("/account-verification")
    public String accountVerification() {
        return "account_verification";
    }

    @GetMapping("/loan-eligibility")
    public String loanEligibility() {
        return "loan_eligibility";
    }

    @GetMapping("/account-balance")
    public String accountBalance() {
        return "account_balance";
    }

    @GetMapping("/transactions")
    public String transactions() {
        return "transactions";
    }

    @GetMapping("/exchange-rates")
    public String exchangeRates() {
        return "exchange_rates";
    }

    @GetMapping("/coming-soon")
    public String comingSoon() {
        return "coming_soon";
    }

    private void addUserAttributes(Model model, OidcUser user) {
        if (user == null || user.getUserInfo() == null) {
            return;
        }
        var info = user.getUserInfo();
        model.addAttribute("name",            str(info.getClaim("name")));
        model.addAttribute("email",           str(info.getClaim("email")));
        model.addAttribute("phone",           str(info.getClaim("phone_number")));
        model.addAttribute("gender",          str(info.getClaim("gender")));
        model.addAttribute("birthdate",       str(info.getClaim("birthdate")));
        model.addAttribute("residenceStatus", str(info.getClaim("nationality")));
        model.addAttribute("address",         str(info.getClaim("address")));
        model.addAttribute("userPicture",     str(info.getClaim("picture")));
        model.addAttribute("sub",             user.getSubject());
    }

    private String str(Object value) {
        return value != null ? value.toString() : "";
    }
}