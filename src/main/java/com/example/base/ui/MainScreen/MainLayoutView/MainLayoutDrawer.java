package com.example.base.ui.MainScreen.MainLayoutView;

import java.util.function.Consumer;

import org.springframework.security.core.Authentication;

import com.example.base.service.GlobalChatService;
import com.example.base.ui.AdminScreen.AdminDashboardView;
import com.example.base.ui.AdminScreen.AdminManagment.AdminManagmentView;
import com.example.base.ui.AdminScreen.AdminSettingsView;
import com.example.base.ui.AdminScreen.AdminUserManagmentView;
import com.example.base.ui.AdminScreen.SystemLogsView;
import com.example.base.ui.Chat.GenelChatView;
import com.example.base.ui.CustomerScreen.CustomerDashboardView;
import com.example.base.ui.CustomerScreen.FaqView;
import com.example.base.ui.CustomerScreen.TalepAcmaView.TalepAcma;
import com.example.base.ui.CustomerScreen.Taleplerim.TaleplerimView;
import com.example.base.ui.HelpDeskerScreen.HelpdeskDashboardView;
import com.example.base.ui.HelpDeskerScreen.MusteriOnayView;
import com.example.base.ui.HelpDeskerScreen.OnInceleme.OnIncelemeView;
import com.example.base.ui.PoScreen.KvkkApprovalView;
import com.example.base.ui.PoScreen.PODashboardView;
import com.example.base.ui.PoScreen.TalepDegerlendirme.TalepDegerlendirmeView;
import com.example.base.ui.ProgrammerScreen.ProgrammerDashboardView;
import com.example.base.ui.ProgrammerScreen.ProgrammerTask.ProgrammerTaskView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;

public class MainLayoutDrawer {

    private final Component context;
    private SideNavItem genelChatNavItem;

    public MainLayoutDrawer(Component context) {
        this.context = context;
    }

    public void buildDrawer(Authentication auth, Consumer<Component> drawerAdder) {
        if (auth == null) return;

        boolean isGod = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_GODPANEL"));

        if (isGod || auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"))) {
            SideNav customerNav = new SideNav();
            if (isGod) customerNav.setLabel("Customer");

            customerNav.addItem(new SideNavItem(context.getTranslation("menu.dashboard"), CustomerDashboardView.class, VaadinIcon.CHART_LINE.create()));
            customerNav.addItem(new SideNavItem(context.getTranslation("menu.newRequest"), TalepAcma.class, VaadinIcon.PLUS.create()));
            customerNav.addItem(new SideNavItem(context.getTranslation("menu.myRequests"), TaleplerimView.class, VaadinIcon.LIST.create()));
            customerNav.addItem(new SideNavItem("Bilgi Bankası", FaqView.class, VaadinIcon.QUESTION_CIRCLE.create()));

            drawerAdder.accept(customerNav);
        }

        if (isGod || auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PROGRAMMER"))) {
            SideNav programmerNav = new SideNav();
            if (isGod) programmerNav.setLabel("Programmer");
            programmerNav.addItem(new SideNavItem(context.getTranslation("menu.programmerTasks"), ProgrammerTaskView.class, VaadinIcon.CODE.create()));
            programmerNav.addItem(new SideNavItem(context.getTranslation("menu.dashboard"), ProgrammerDashboardView.class, VaadinIcon.CHART_LINE.create()));

            genelChatNavItem = createGenelChatItem();
            programmerNav.addItem(genelChatNavItem);

            drawerAdder.accept(programmerNav);
        }

        if (isGod || auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            SideNav adminNav = new SideNav();
            if (isGod) adminNav.setLabel("Admin");
            adminNav.addItem(new SideNavItem(context.getTranslation("menu.userManagement"), AdminUserManagmentView.class, VaadinIcon.USERS.create()));
            adminNav.addItem(new SideNavItem(context.getTranslation("menu.systemLogs"), SystemLogsView.class, VaadinIcon.CHART_LINE.create()));
            adminNav.addItem(new SideNavItem(context.getTranslation("menu.dashboard"), AdminDashboardView.class, VaadinIcon.LIST_SELECT.create()));
            adminNav.addItem(new SideNavItem(context.getTranslation("menu.systemManagement"), AdminManagmentView.class, VaadinIcon.LIST.create()));
            adminNav.addItem(new SideNavItem(context.getTranslation("menu.systemSettings"), AdminSettingsView.class, VaadinIcon.COG.create()));

            genelChatNavItem = createGenelChatItem();
            adminNav.addItem(genelChatNavItem);

            drawerAdder.accept(adminNav);
        }

        if (isGod || auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HELPDESK"))) {
            SideNav helpdeskNav = new SideNav();
            if (isGod) helpdeskNav.setLabel("Helpdesk");
            helpdeskNav.addItem(new SideNavItem(context.getTranslation("menu.incomingRequests"), OnIncelemeView.class, VaadinIcon.INBOX.create()));
            helpdeskNav.addItem(new SideNavItem(context.getTranslation("menu.customerApprovals"), MusteriOnayView.class, VaadinIcon.USER_CHECK.create()));
            helpdeskNav.addItem(new SideNavItem(context.getTranslation("menu.performanceReport"), HelpdeskDashboardView.class, VaadinIcon.CHART_LINE.create()));

            genelChatNavItem = createGenelChatItem();
            helpdeskNav.addItem(genelChatNavItem);

            drawerAdder.accept(helpdeskNav);
        }

        if (isGod || auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PO"))) {
            SideNav poNav = new SideNav();
            if (isGod) poNav.setLabel("PO");
            poNav.addItem(new SideNavItem(context.getTranslation("menu.pendingRequests"), TalepDegerlendirmeView.class, VaadinIcon.LIST_SELECT.create()));
            poNav.addItem(new SideNavItem(context.getTranslation("menu.dashboard"), PODashboardView.class, VaadinIcon.CHART_LINE.create()));
            poNav.addItem(new SideNavItem(context.getTranslation("menu.kvkkRequests"), KvkkApprovalView.class, VaadinIcon.NOTEBOOK.create()));

            genelChatNavItem = createGenelChatItem();
            poNav.addItem(genelChatNavItem);

            drawerAdder.accept(poNav);
        }
    }

    private SideNavItem createGenelChatItem() {
        SideNavItem item = new SideNavItem("Genel Sohbet", GenelChatView.class);
        item.setPrefixComponent(VaadinIcon.COMMENTS.create());
        return item;
    }

    public void updateGlobalChatBadge(Integer currentUserId, GlobalChatService globalChatService) {
        if (currentUserId == null || genelChatNavItem == null) return;

        int unreadCount = globalChatService.getUnreadCountForUser(currentUserId);

        if (unreadCount > 0) {
            Badge badge = new Badge(String.valueOf(unreadCount));
            badge.addThemeVariants(BadgeVariant.ERROR);
            badge.addClassName("main-layout-chat-badge");
            genelChatNavItem.setSuffixComponent(badge);
        } else {
            genelChatNavItem.setSuffixComponent(null);
        }
    }
}