# Unify Chat Roles Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Unify the chat flow across PARENT, STUDENT, and TEACHER roles so they all use the real backend-connected `_ServiceConversationsScreen` when `chatService` is active.

**Architecture:** Update `ConversationsScreen` to return `_ServiceConversationsScreen` for all actors when `chatService` is initialized. Pass the `actor` parameter to `_ServiceConversationsScreen` and conditionally display the app bar title ("Tin nhắn phụ huynh" for TEACHER, "Tin nhắn liên lạc" for PARENT/STUDENT).

**Tech Stack:** Flutter, Dart

---

### Phase 1: Update ConversationsScreen Route Mapping

**Files:**
- Modify: `lib/vn/edu/fpt/view/screens/messages_screen.dart`

- [ ] **Step 1: Modify build method in ConversationsScreen**
  Change lines 21-30 in `ConversationsScreen` to return `_ServiceConversationsScreen` with the `actor` argument if `chatService` is not null. Only fall back to `TeacherInboxScreen()` if `chatService` is null and `actor` is `teacher`.

  *Target code change in `ConversationsScreen.build`:*
  ```dart
  @override
  Widget build(BuildContext context) {
    final service = chatService;
    if (service != null) {
      return _ServiceConversationsScreen(chatService: service, actor: actor);
    }

    if (actor == AppActor.teacher) {
      return const TeacherInboxScreen();
    }
  ```

---

### Phase 2: Add actor Parameter to _ServiceConversationsScreen and Customize Title

**Files:**
- Modify: `lib/vn/edu/fpt/view/screens/messages_screen.dart`

- [ ] **Step 1: Update constructor of _ServiceConversationsScreen**
  Modify the `_ServiceConversationsScreen` class declaration and constructor to accept `AppActor actor`.

  *Target code change:*
  ```dart
  class _ServiceConversationsScreen extends StatelessWidget {
    const _ServiceConversationsScreen({required this.chatService, this.actor = AppActor.parent});

    final ChatService chatService;
    final AppActor actor;
  ```

- [ ] **Step 2: Conditional Title in AppBar**
  Modify the `OrangeTopBar` instantiation inside `_ServiceConversationsScreen.build` to set the title dynamically.

  *Target code change:*
  ```dart
  appBar: OrangeTopBar(
    title: actor == AppActor.teacher ? 'Tin nhắn phụ huynh' : 'Tin nhắn liên lạc',
    actions: [
  ```

---

### Phase 3: Verification & Test Execution

**Files:**
- Test: `test/chat_ui_screen_test.dart`

- [ ] **Step 1: Verify all tests still pass**
  Run the test suite to ensure the existing tests (which test `ConversationsScreen` with `AppActor.parent`) continue to pass.
