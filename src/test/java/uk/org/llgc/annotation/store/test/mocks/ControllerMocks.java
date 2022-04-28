package uk.org.llgc.annotation.store.test.mocks;

import uk.org.llgc.annotation.store.data.users.User;
import uk.org.llgc.annotation.store.controllers.StoreService;

public class ControllerMocks {

    public static StoreService getStoreServiceWithUser(final User pUser) {
        StoreService tService = new StoreService() {
            public User getCurrentUser() {
                return pUser;
            }
        };
        tService.init();

        return tService;
    }
}
