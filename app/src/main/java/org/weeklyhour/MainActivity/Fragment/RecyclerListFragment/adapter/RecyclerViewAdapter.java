package org.weeklyhour.MainActivity.Fragment.RecyclerListFragment.adapter;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import com.bignerdranch.expandablerecyclerview.Adapter.ExpandableRecyclerAdapter;
import com.bignerdranch.expandablerecyclerview.Model.ParentListItem;

import org.weeklyhour.MainActivity.Fragment.RecyclerListFragment.Item.childItem;
import org.weeklyhour.MainActivity.Fragment.RecyclerListFragment.Item.parentItem;
import org.weeklyhour.MainActivity.Fragment.RecyclerListFragment.ViewHolder.childViewHolder;
import org.weeklyhour.MainActivity.Fragment.RecyclerListFragment.ViewHolder.parentViewHolder;
import org.weeklyhour.MainActivity.R;

import java.util.List;
import java.util.Random;

import io.realm.Realm;

/**
 * 어댑터는 RecyclerView에게 데이터를 전달해주는 역할을 한다.
 * 최초 선언시 RecyclerView에서 사용될 ArrayList<parentItem> 을 갖는다.
 * Created by admin on 2016-09-07.
 */
public class RecyclerViewAdapter extends ExpandableRecyclerAdapter<parentViewHolder, childViewHolder> implements setRealmResultClearCallback{
    private static final Random rnd = new Random();
    private List<? extends ParentListItem> mParentItems;
    private final Realm realm = Realm.getDefaultInstance();


    public RecyclerViewAdapter(@NonNull List<? extends ParentListItem> parentItemList) {
        super(parentItemList);
        mParentItems = parentItemList;
    }

    @Override
    public parentViewHolder onCreateParentViewHolder(ViewGroup parentViewGroup) {
        View parentView = LayoutInflater.from(parentViewGroup.getContext())
                .inflate(R.layout.fragment_recycler_parent_item, parentViewGroup, false);
        return new parentViewHolder(parentView);
    }

    @Override
    public childViewHolder onCreateChildViewHolder(ViewGroup childViewGroup) {
        View childView = LayoutInflater.from(childViewGroup.getContext())
                .inflate(R.layout.fragment_recycler_child_item, childViewGroup, false);
        return new childViewHolder(childView);
    }

    @Override
    public void onBindParentViewHolder(final parentViewHolder parentViewHolder, int position, final ParentListItem parentListItem) {
        parentItem pItem = (parentItem) parentListItem;

        //배경은 흰색으로 고정
        parentViewHolder.progressbar.setProgressBackgroundColor(Color.parseColor("#FFFFFF"));

        //Adapter에 적용된 ArrayList에서 순차적으로 꺼내와서 실제 객체에 적용
        parentViewHolder.progressbar.setProgressColor(pItem.progressBarColor);
        parentViewHolder.progressbar.setMax(pItem.maxDay);

        /*
        TODO 원래는 currDay 를 넣어야 하지만 BackGroundColor 확인을 위해 랜덤값.
        이 값은 onPause 이하로 라이프사이클이 내려갈때 다시 갱신되더라.
        */
        parentViewHolder.progressbar.setProgress(rnd.nextInt(pItem.maxDay + 1));

        parentViewHolder.day.setText((int)parentViewHolder.progressbar.getProgress() + " / " + pItem.maxDay);
        parentViewHolder.taskName.setText(pItem.taskName);

        //toggleImage의 경우 protected method때문에 new ParentViewHolder에서 정의됨
    }

    @Override
    public void onBindChildViewHolder(final childViewHolder childViewHolder, final int position, Object childListItem) {
        childItem cItem = (childItem) childListItem;

        childViewHolder.memo.setText(cItem.memo);
        childViewHolder.startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TODO startButton SetOnClickListener
            }
        });
        childViewHolder.memo.setEnabled(false);

        //EDIT button onClickListener
        childViewHolder.editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(childViewHolder.memo.isEnabled()){
                    //이미 Edit 모드일 경우. Disable TextView, Realm update
                    childViewHolder.memo.setEnabled(false);
                    childViewHolder.editButton.setText("EDIT");

                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            childItem cItem = (childItem)mParentItems.get(position - 1).getChildItemList().get(0);
                            cItem.memo = childViewHolder.memo.getText().toString();
                        }
                    });
                }
                else{
                    //Edit 모드가 아니었을 경우(기본), 버튼텍스트를 변경하고 소프트키보드를 띄움
                    childViewHolder.memo.setEnabled(true);
                    childViewHolder.editButton.setText("DONE!");

                    childViewHolder.memo.requestFocus();//포커스를 옮김
                    childViewHolder.memo.setSelection(childViewHolder.memo.getText().length());//커서를 맨 뒤로 옮김

                    //키보드를 띄움
                    InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
                }
            }
        });
    }

    public void addItem(final String taskName, final int maxDay, final int color, final String memo){

        realm.executeTransactionAsync(new Realm.Transaction(){
            @Override
            public void execute(Realm realm) {
                parentItem pItem = realm.createObject(parentItem.class);

                //Primary key increment
                pItem.id = realm.where(parentItem.class).max("id").intValue() + 1;
                pItem.taskName = taskName;
                pItem.maxDay = maxDay;
                pItem.progressBarColor = color;
            }
        }, new Realm.Transaction.OnSuccess(){
            @Override
            public void onSuccess() {
                //정상적으로 parentItem이 삽입된 경우
                realm.executeTransactionAsync(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        childItem cItem = realm.createObject(childItem.class);
                        cItem.id = realm.where(childItem.class).max("id").intValue() + 1;
                        cItem.memo = memo;
                    }
                }, new Realm.Transaction.OnSuccess() {
                    @Override
                    public void onSuccess() {
                        notifyParentItemInserted(mParentItems.size() - 1);
                    }
                });
            }
        });
    }

    @Override
    public void setDataInit() {
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                notifyParentItemRangeRemoved(0, mParentItems.size());
                realm.deleteAll();
            }
        });
    }
}
