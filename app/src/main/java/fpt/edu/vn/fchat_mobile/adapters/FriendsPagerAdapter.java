package fpt.edu.vn.fchat_mobile.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import fpt.edu.vn.fchat_mobile.fragments.FriendListFragment;
import fpt.edu.vn.fchat_mobile.fragments.FriendRequestFragment;

public class FriendsPagerAdapter extends FragmentStateAdapter {
    private String currentUserId;

    public FriendsPagerAdapter(@NonNull FragmentActivity fragmentActivity, String currentUserId) {
        super(fragmentActivity);
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return FriendListFragment.newInstance(currentUserId);
            case 1:
                return FriendRequestFragment.newInstance(currentUserId);
            default:
                return FriendListFragment.newInstance(currentUserId);
        }
    }

    @Override
    public int getItemCount() {
        return 2; // Two tabs: Friends and Requests
    }
}
