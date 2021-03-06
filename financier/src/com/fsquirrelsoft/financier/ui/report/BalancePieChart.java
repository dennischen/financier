package com.fsquirrelsoft.financier.ui.report;

import android.content.Context;
import android.content.Intent;

import com.fsquirrelsoft.financier.data.AccountType;
import com.fsquirrelsoft.financier.data.Balance;

import org.achartengine.ChartFactory;
import org.achartengine.model.CategorySeries;
import org.achartengine.renderer.DefaultRenderer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.List;

public class BalancePieChart extends AbstractChart {

    DecimalFormat percentageFormat = new DecimalFormat("##0");

    public BalancePieChart(Context context, int orientation, float dpRatio) {
        super(context, orientation, dpRatio);
    }

    public Intent createIntent(AccountType at, List<Balance> balances) {
        BigDecimal total = BigDecimal.ZERO;
        for (Balance b : balances) {
            total = total.add(b.getMoney().compareTo(BigDecimal.ZERO) <= 0 ? BigDecimal.ZERO : b.getMoney());
        }
        CategorySeries series = new CategorySeries(at.getDisplay(i18n));
        for (Balance b : balances) {
            if (b.getMoney().compareTo(BigDecimal.ZERO) > 0) {
                StringBuilder sb = new StringBuilder();
                sb.append(b.getName());
                BigDecimal p = b.getMoney().multiply(new BigDecimal("100")).divide(total, 2, RoundingMode.HALF_UP);
                if (p.compareTo(BigDecimal.ONE) >= 0) {
                    sb.append("(").append(percentageFormat.format(p)).append("%)");
                    series.add(sb.toString(), b.getMoney().doubleValue());
                }
            }
        }
        int[] color = createColor(series.getItemCount());
        DefaultRenderer renderer = buildCategoryRenderer(color);
        renderer.setLabelsTextSize(14 * dpRatio);
        renderer.setLegendTextSize(16 * dpRatio);
        return ChartFactory.getPieChartIntent(context, series, renderer, series.getTitle());
    }
}
